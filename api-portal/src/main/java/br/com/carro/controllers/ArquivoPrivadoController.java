package br.com.carro.controllers;

import br.com.carro.autenticacao.JpaUserDetailsService;
import br.com.carro.entities.DTO.ItemDTO;
import br.com.carro.entities.Usuario.Usuario;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/arquivos/privado")
@CrossOrigin(origins = "*") // Para permitir requisições do front-end

public class ArquivoPrivadoController {
    private static final Logger logger = LoggerFactory.getLogger(ArquivoPrivadoController.class);

    @Value("${app.file.upload-dir}")
    private String pastaRaizPublica; // Renomeado para pastaRaizUsuario

    private final JpaUserDetailsService userDetailsService;

    @Autowired
    public ArquivoPrivadoController(@Value("${app.file.upload-dir}") String pastaRaizPublica, JpaUserDetailsService userDetailsService) {
        this.pastaRaizPublica = pastaRaizPublica;
        this.userDetailsService = userDetailsService;
        logger.info("O ArquivoPrivadoController foi inicializado com a pasta raiz: {}" + this.pastaRaizPublica);
    }

    /**
     * Resolve o caminho do arquivo garantindo que o usuário não possa sair do seu diretório.
     * @param jwt O token JWT do usuário autenticado.
     * @param caminho O caminho relativo do arquivo/pasta.
     * @return O caminho absoluto validado.
     */
    private Path resolveCaminhoDoUsuario(Jwt jwt, String caminho) throws EntityNotFoundException {
        if (jwt == null || !jwt.hasClaim("sub")) {
            throw new EntityNotFoundException("Usuário não autenticado.");
        }
        Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
        String pastaUsuario = Paths.get(pastaRaizPublica, "usuarios", usuarioLogado.getId().toString()).toString();

        String caminhoLimpo = (caminho == null || caminho.trim().isEmpty()) ? "" : caminho.replace("/", File.separator);
        Path caminhoValidado = Paths.get(pastaUsuario).resolve(caminhoLimpo);

        // Verifica se o caminho normalizado começa com o caminho da pasta do usuário.
        // Isso evita "Directory Traversal Attacks".
        if (!caminhoValidado.normalize().startsWith(Paths.get(pastaUsuario).normalize())) {
            logger.warn("Tentativa de acesso não autorizado pelo usuário " + usuarioLogado.getId() + " ao caminho: " + caminhoValidado.toAbsolutePath());
            throw new SecurityException("Tentativa de acesso não autorizado.");
        }

        return caminhoValidado;
    }

    /**
     * Lista o conteúdo de um diretório específico para o usuário autenticado.
     */
    @GetMapping("teste")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<List<ItemDTO>> listarConteudo(@RequestParam(required = false) String caminho, @AuthenticationPrincipal Jwt jwt) {
        try {
            Path caminhoValidado = resolveCaminhoDoUsuario(jwt, caminho);

            File diretorio = caminhoValidado.toFile();
            if (!diretorio.exists() || !diretorio.isDirectory()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
            }

            return ResponseEntity.ok(listarConteudoRecursivamente(diretorio));
        } catch (EntityNotFoundException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        }
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
     * Faz o download de um arquivo para o usuário autenticado.
     */
    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Resource> downloadArquivo(@RequestParam String caminho, @AuthenticationPrincipal Jwt jwt) throws IOException {
        try {
            Path caminhoValidado = resolveCaminhoDoUsuario(jwt, caminho);
            Path arquivo = caminhoValidado;
            Resource resource = new UrlResource(arquivo.toUri());

            if (resource.exists() && resource.isReadable() && !Files.isDirectory(arquivo)) {
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getFileName().toString() + "\"");
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (EntityNotFoundException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Visualiza um arquivo no navegador para o usuário autenticado.
     */
    @GetMapping("/view")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Resource> visualizarArquivo(@RequestParam String caminho, @AuthenticationPrincipal Jwt jwt) throws IOException {
        try {
            Path caminhoValidado = resolveCaminhoDoUsuario(jwt, caminho);

            Path arquivo = caminhoValidado;
            Resource resource = new UrlResource(arquivo.toUri());

            if (resource.exists() && resource.isReadable() && !Files.isDirectory(arquivo)) {
                String mimeType = Files.probeContentType(arquivo);
                MediaType contentType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;

                return ResponseEntity.ok()
                        .contentType(contentType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (EntityNotFoundException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
