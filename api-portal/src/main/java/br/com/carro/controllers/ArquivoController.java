package br.com.carro.controllers;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.DTO.ArquivoMoverDTO;
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
import org.springframework.security.access.AccessDeniedException; // ✅ Importa a exceção correta
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
     * Endpoint para upload de um ou mais arquivos.
     * Unifica o upload de arquivo único e múltiplos arquivos.
     */
    @PostMapping(value = "/upload-multiplo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> uploadArquivos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            if (files.length > 1) {
                List<ArquivoDTO> arquivosSalvos = arquivoService.uploadMultiplosArquivos(Arrays.asList(files), pastaId, usuarioLogado);
                return ResponseEntity.status(HttpStatus.CREATED).body(arquivosSalvos);
            } else if (files.length == 1) {
                List<ArquivoDTO> arquivosSalvos = arquivoService.uploadArquivo(files[0], pastaId, usuarioLogado);
                return ResponseEntity.status(HttpStatus.CREATED).body(arquivosSalvos);
            } else {
                return ResponseEntity.badRequest().body("Nenhum arquivo enviado.");
            }
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    // ✅ NOVO ENDPOINT DE UPLOAD ÚNICO PARA SIMPLICIDADE.
    // Pode ser removido se o endpoint acima já for suficiente.
    @PostMapping(value = "/upload-unico", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> uploadArquivoUnico(
            @RequestParam("files") MultipartFile file,
            @RequestParam("pastaId") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            List<ArquivoDTO> arquivosSalvos = arquivoService.uploadArquivo(file, pastaId, usuarioLogado);
            return ResponseEntity.status(HttpStatus.CREATED).body(arquivosSalvos);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }


    /**
     * Lista todos os arquivos dentro de uma pasta específica.
     * Agora retorna uma lista de DTOs.
     */
    @GetMapping("/pasta/{pastaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<?> listarArquivosPorPasta(
            @PathVariable Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            List<ArquivoDTO> arquivos = arquivoService.listarArquivosPorPasta(pastaId, usuarioLogado);
            return ResponseEntity.ok(arquivos);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Endpoint para download de um arquivo pelo seu ID.
     */
    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Resource> downloadArquivo(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            Arquivo arquivo = arquivoService.getArquivoComAcesso(id, usuarioLogado);

            if (arquivo == null) {
                System.err.println("Arquivo com ID " + id + " não encontrado.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String nomeOriginal = arquivo.getNomeArquivo();

                // Determina o Content-Type (com fallback)
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    System.err.println("Não foi possível determinar o Content-Type. Usando 'application/octet-stream'.");
                    contentType = "application/octet-stream";
                }

                // Garante que o nome tenha extensão (caso não tenha)
                if (!nomeOriginal.contains(".")) {
                    String guessedExt = contentType.equals("application/pdf") ? ".pdf"
                            : contentType.startsWith("image/") ? "." + contentType.split("/")[1]
                            : ".bin"; // fallback
                    nomeOriginal += guessedExt;
                }

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeOriginal + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                System.err.println("Recurso não existe ou não pode ser lido: " + filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (EntityNotFoundException | AccessDeniedException e) {
            System.err.println("Erro de permissão ou entidade não encontrada: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            System.err.println("Erro de E/S ou URL malformada: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Atualiza os metadados de um arquivo por ID.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> atualizarMetadados(
            @PathVariable Long id,
            @RequestBody ArquivoUpdateDTO dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            ArquivoDTO arquivoAtualizado = arquivoService.atualizarMetadados(id, dto, usuarioLogado);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Substitui o conteúdo de um arquivo por uma nova versão.
     */
    @PutMapping("/{id}/substituir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ArquivoDTO> substituirArquivo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            ArquivoDTO arquivoAtualizado = arquivoService.substituirArquivo(id, file, usuarioLogado);
            return ResponseEntity.ok(arquivoAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //--- ENDPOINT PARA MOVER UM ARQUIVO-----

    @PutMapping("/mover/{arquivoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')") // Adicione a anotação de segurança
    public ResponseEntity<String> moverArquivo(
            @PathVariable Long arquivoId,
            @RequestBody ArquivoMoverDTO moverArquivoDTO,
            @AuthenticationPrincipal Jwt jwt) { // Injete o objeto Jwt
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject()); // Busque o usuário
            arquivoService.moverArquivo(arquivoId, moverArquivoDTO.getPastaDestinoId(), usuarioLogado);
            return ResponseEntity.ok("Arquivo movido com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao mover arquivo: " + e.getMessage());
        }
    }


    //--- ENDPOINT PARA MOVER UM ARQUIVO-----

    /**
     * Exclui um arquivo pelo seu ID.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirArquivo(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            arquivoService.excluirArquivo(id, usuarioLogado);
            return ResponseEntity.ok("Arquivo com ID " + id + " excluído com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao excluir o arquivo do disco.");
        }
    }

    /**
     * Exclui múltiplos arquivos por uma lista de IDs.
     */
    @DeleteMapping("/excluir-multiplos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirMultiplosArquivos(
            @RequestBody List<Long> arquivoIds,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            arquivoService.excluirMultiplosArquivos(arquivoIds, usuarioLogado);
            return ResponseEntity.ok("Arquivos excluídos com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao excluir arquivos do disco.");
        }
    }

    /**
     * Exclui todos os arquivos de uma pasta por ID.
     */
    @DeleteMapping("/pasta/{id}/excluir-arquivos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<String> excluirTodosArquivosNaPasta(
            @PathVariable("id") Long pastaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
            arquivoService.excluirTodosArquivosNaPasta(pastaId, usuarioLogado);
            return ResponseEntity.ok("Todos os arquivos da pasta foram excluídos com sucesso.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
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
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());
        Sort sortObj = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ArquivoDTO> arquivos = arquivoService.buscarPorNome(nome, pageable, usuarioLogado);
        return ResponseEntity.ok(arquivos);
    }
}