package br.com.carro.controllers;

import br.com.carro.entities.DTO.ItemDTO;
import br.com.carro.services.PastaService;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/privado/pastas")
@CrossOrigin(origins = "*")
public class PastaPrivadoController {

    private static final Logger logger = LoggerFactory.getLogger(PastaPrivadoController.class);

    @Value("${app.file.upload-dir}")
    private String pastaRaizPrivada;

    private final PastaService pastaService; // ⬅️ Serviço que lida com banco de dados para pastas/arquivos

    public PastaPrivadoController(PastaService pastaService,
                                  @Value("${app.file.upload-dir}") String pastaRaizPrivada) {
        this.pastaService = pastaService;
        this.pastaRaizPrivada = pastaRaizPrivada;
        logger.info("✅ PastaPrivadoController inicializado com pasta raiz: {}", this.pastaRaizPrivada);
    }

    /**
     * Lista o conteúdo de uma pasta privada (sincronizado com o banco de dados).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','BASIC')")
    public List<ItemDTO> listarConteudo(@RequestParam(required = false) String caminho) {
        String caminhoParaListar = (caminho == null || caminho.trim().isEmpty()) ? "" : caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaRaizPrivada).resolve(caminhoParaListar);

        logger.info("📂 Listando conteúdo privado para caminho: {}", caminhoValidado.toAbsolutePath());

        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaRaizPrivada).normalize())) {
            logger.warn("🚫 Tentativa de acesso não autorizado: {}", caminhoValidado.toAbsolutePath());
            return new ArrayList<>();
        }

//        // ✅ Sincroniza com banco de dados antes de listar
//        pastaService.sincronizarComBanco(caminhoValidado);

        File diretorio = caminhoValidado.toFile();
        if (!diretorio.exists() || !diretorio.isDirectory()) {
            logger.warn("⚠️ Diretório não encontrado ou inválido: {}", diretorio.getAbsolutePath());
            return new ArrayList<>();
        }

        return listarConteudoRecursivamente(diretorio);
    }

    private List<ItemDTO> listarConteudoRecursivamente(File diretorio) {
        File[] files = diretorio.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<ItemDTO> items = new ArrayList<>();
        Arrays.sort(files);

        for (File file : files) {
            if (file.isDirectory()) {
                List<ItemDTO> filhos = listarConteudoRecursivamente(file);
                double tamanhoTotal = filhos.stream().mapToDouble(ItemDTO::tamanho).sum();
                int contagem = filhos.size();
                items.add(new ItemDTO(file.getName(), true, filhos, tamanhoTotal, contagem));
            } else {
                double sizeInKB = file.length() / 1024.0;
                items.add(new ItemDTO(file.getName(), false, null, sizeInKB, null));
            }
        }
        return items;
    }

    /**
     * Baixar arquivo privado com validação de permissões.
     */
    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','BASIC')")
    public ResponseEntity<Resource> downloadArquivo(@RequestParam String caminho) throws IOException {
        String caminhoLimpo = caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaRaizPrivada).resolve(caminhoLimpo);

        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaRaizPrivada).normalize())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path arquivo = caminhoValidado;
        Resource resource = new UrlResource(arquivo.toUri());

        if (resource.exists() || resource.isReadable()) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getFileName().toString() + "\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Visualizar arquivo privado no navegador.
     */
    @GetMapping("/view")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','BASIC')")
    public ResponseEntity<Resource> visualizarArquivo(@RequestParam String caminho) throws IOException {
        String caminhoLimpo = caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaRaizPrivada).resolve(caminhoLimpo);

        logger.info("👁️ Tentando visualizar arquivo privado: {}", caminhoValidado.toAbsolutePath());

        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaRaizPrivada).normalize())) {
            logger.warn("🚫 Tentativa de acesso não autorizado para visualização: {}", caminhoValidado.toAbsolutePath());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path arquivo = caminhoValidado;
        Resource resource = new UrlResource(arquivo.toUri());

        if (resource.exists() || resource.isReadable()) {
            String mimeType = Files.probeContentType(arquivo);
            MediaType contentType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
