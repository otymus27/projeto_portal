package br.com.carro.controllers;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.DTO.ArquivoUpdateDTO;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.services.ArquivoService;
import br.com.carro.autenticacao.JpaUserDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Arrays;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/arquivo")
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
            @RequestParam("files") MultipartFile file,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());

            // ✅ Agora o serviço retorna um DTO
            ArquivoDTO arquivoSalvoDto = arquivoService.uploadArquivo(file, pastaId, usuarioLogado);

            return ResponseEntity.status(HttpStatus.CREATED).body(arquivoSalvoDto);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar o arquivo: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * ✅ NOVO ENDPOINT: Upload de múltiplos arquivos.
     */
    @PostMapping(value = "/upload-multiplos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> uploadMultiplosArquivos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            List<ArquivoDTO> arquivosSalvos = arquivoService.uploadMultiplosArquivos(Arrays.asList(files), pastaId, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(arquivosSalvos);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar os arquivos: " + e.getMessage());
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
     * Atualiza os metadados de um arquivo por ID.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> atualizarMetadados(@PathVariable Long id, @RequestBody ArquivoUpdateDTO dto) {
        try {
            ArquivoDTO arquivoAtualizado = arquivoService.atualizarMetadados(id, dto);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Substitui o conteúdo de um arquivo por uma nova versão.
     */
    @PostMapping("/{id}/substituir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> substituirArquivo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            ArquivoDTO arquivoAtualizado = arquivoService.substituirArquivo(id, file);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
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
        } catch (EntityNotFoundException | IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Exclui múltiplos arquivos por uma lista de IDs.
     */
    @DeleteMapping("/excluir-multiplos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirMultiplosArquivos(@RequestBody List<Long> arquivoIds) {
        try {
            arquivoService.excluirMultiplosArquivos(arquivoIds);
            return ResponseEntity.ok("Arquivos excluídos com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao excluir arquivos do disco.");
        }
    }

    /**
     * Exclui todos os arquivos de uma pasta por ID.
     */
    @DeleteMapping("/pasta/{id}/excluir-arquivos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirTodosArquivosNaPasta(@PathVariable("id") Long pastaId) {
        try {
            arquivoService.excluirTodosArquivosNaPasta(pastaId);
            return ResponseEntity.ok("Todos os arquivos da pasta foram excluídos com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao excluir arquivos do disco.");
        }
    }

    /**
     * Busca arquivos por nome em qualquer parte.
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Page<ArquivoDTO>> buscarArquivosPorNome(
            @RequestParam("nome") String nome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nomeArquivo") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sortObj = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ArquivoDTO> arquivos = arquivoService.buscarPorNome(nome, pageable);
        return ResponseEntity.ok(arquivos);
    }

}