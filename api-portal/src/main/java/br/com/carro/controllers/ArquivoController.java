package br.com.carro.controllers;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.services.ArquivoService;
import br.com.carro.autenticacao.JpaUserDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/arquivos")
public class ArquivoController {

    private final ArquivoService arquivoService;
    private final JpaUserDetailsService userDetailsService;

    @Autowired
    public ArquivoController(ArquivoService arquivoService, JpaUserDetailsService userDetailsService) {
        this.arquivoService = arquivoService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Endpoint para upload de arquivos para uma pasta específica.
     * Agora passa o usuário logado para o serviço.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> uploadArquivo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            // ✅ Agora passa o usuário para o serviço
            Arquivo arquivoSalvo = arquivoService.salvarArquivo(file, pastaId, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(arquivoSalvo);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar o arquivo: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Lista todos os arquivos dentro de uma pasta específica.
     * Agora passa o usuário logado para o serviço para a verificação de permissão.
     */
    @GetMapping("/pasta/{pastaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<List<Arquivo>> listarArquivosPorPasta(
            @PathVariable Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            // ✅ Agora passa o usuário para o serviço
            List<Arquivo> arquivos = arquivoService.listarArquivosPorPasta(pastaId, usuarioLogado);
            return ResponseEntity.ok(arquivos);
        } catch (EntityNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Endpoint para download de um arquivo pelo seu ID.
     * Agora passa o usuário logado para o serviço para a verificação de permissão.
     */
    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Resource> downloadArquivo(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            // ✅ Usa o novo método do serviço que já inclui a verificação
            Arquivo arquivo = arquivoService.buscarPorId(id, usuarioLogado);

            Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getNomeArquivo() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (MalformedURLException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exclui um arquivo pelo seu ID.
     * Agora passa o usuário logado para o serviço para a verificação de permissão.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirArquivo(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            // ✅ Agora passa o usuário para o serviço
            arquivoService.excluirArquivo(id, usuarioLogado);
            return ResponseEntity.ok("Arquivo com ID " + id + " excluído com sucesso.");
        } catch (EntityNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}