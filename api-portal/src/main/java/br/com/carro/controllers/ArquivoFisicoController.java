package br.com.carro.controllers;

import br.com.carro.entities.DTO.ItemDTO;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/publico/arquivos")
@CrossOrigin(origins = "*")
public class ArquivoFisicoController {

    private static final Logger logger = LoggerFactory.getLogger(ArquivoFisicoController.class);

    @Value("${app.file.upload-dir}")
    private String pastaRaizPublica;

    public ArquivoFisicoController(@Value("${app.file.upload-dir}") String pastaRaizPublica) {
        this.pastaRaizPublica = pastaRaizPublica;
        logger.info("O ArquivoFisicoController foi inicializado com a pasta raiz: {}" + this.pastaRaizPublica);
    }

    @GetMapping
    public List<ItemDTO> listarConteudo(@RequestParam(required = false) String caminho) {
        String caminhoParaListar = (caminho == null || caminho.trim().isEmpty()) ? "" : caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaRaizPublica).resolve(caminhoParaListar);

        logger.info("Tentando listar o conteúdo do caminho: {}" + caminhoValidado.toAbsolutePath());

        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaRaizPublica).normalize())) {
            logger.warn("Tentativa de acesso não autorizado: {}" + caminhoValidado.toAbsolutePath());
            return new ArrayList<>();
        }

        File diretorio = caminhoValidado.toFile();
        if (!diretorio.exists() || !diretorio.isDirectory()) {
            logger.warn("Diretório não encontrado ou não é um diretório: {}" + diretorio.getAbsolutePath());
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

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadArquivo(@RequestParam String caminho) throws IOException {
        String caminhoLimpo = caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaRaizPublica).resolve(caminhoLimpo);

        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaRaizPublica).normalize())) {
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
     * ✅ Novo método para visualizar o arquivo no navegador.
     * Serve o arquivo com o MediaType correto e sem o cabeçalho Content-Disposition,
     * permitindo que o navegador decida como exibi-lo (e.g., PDF, imagem).
     */
    @GetMapping("/view")
    public ResponseEntity<Resource> visualizarArquivo(@RequestParam String caminho) throws IOException {
        String caminhoLimpo = caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaRaizPublica).resolve(caminhoLimpo);

        logger.info("Tentando visualizar o arquivo: {}" + caminhoValidado.toAbsolutePath());

        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaRaizPublica).normalize())) {
            logger.warn("Tentativa de acesso não autorizado para visualização: {}" + caminhoValidado.toAbsolutePath());
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
