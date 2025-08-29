package br.com.carro.controllers;

import br.com.carro.autenticacao.JpaUserDetailsService;
import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.services.PastaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pasta")
public class PastaController {

    private static final Logger logger = LoggerFactory.getLogger(PastaController.class);
    public record Mensagem(String mensagem) {}

    @Autowired
    private final PastaService pastaService;
    private final JpaUserDetailsService userDetailsService; // ✅ Injetando o service de usuário

    @Autowired
    public PastaController(PastaService pastaService, JpaUserDetailsService userDetailsService) {
        this.pastaService = pastaService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Lista as pastas principais (raiz) do setor do usuário logado.
     * Se o usuário for ADMIN, ele verá todas as pastas.
     */
    @GetMapping("/principais")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Page<Pasta>> listarPastasPrincipaisDoUsuario(
            @AuthenticationPrincipal Jwt jwt, // ✅ Injetando o objeto Jwt
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "nomePasta") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        if (jwt == null || !jwt.hasClaim("sub")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // ✅ Carrega a entidade Usuario completa do banco de dados
        Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());


        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // ✅ Lógica de verificação do papel (role) do usuário
        boolean isAdmin = usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"));

        // ✅ A chamada ao serviço agora passa a informação sobre o papel
        // O Service decide qual método do Repositorio chamar com base nesta flag.
        Page<Pasta> pastas = pastaService.listarPastasPrincipais(
                isAdmin,
                usuarioLogado,
                pageable
        );

        return ResponseEntity.ok(pastas);
    }

    /**
     * Lista as subpastas de uma pasta pai específica.
     * Acesso permitido para 'ADMIN' e 'GERENTE'.
     */
    @GetMapping("/subpastas/{pastaPaiId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Page<Pasta>> listarSubpastas(
            @PathVariable Long pastaPaiId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "nomePasta") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Pasta> subpastas = pastaService.listarSubpastas(pastaPaiId, pageable);
        return ResponseEntity.ok(subpastas);
    }

    /**
     * Busca uma pasta por ID.
     * Acesso para 'ADMIN', 'BASIC' e 'GERENTE'.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BASIC', 'GERENTE')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            Pasta pasta = pastaService.buscarPorId(id);
            return new ResponseEntity<>(pasta, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Cria uma nova pasta. Ela pode ser uma pasta raiz (sem pastaPai) ou uma subpasta.
     * Acesso restrito a 'ADMIN'.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> criarPasta(@RequestBody PastaRequestDTO pastaDTO) {
        try {
            Pasta novaPasta = this.pastaService.criarPastaFromDto(pastaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaPasta);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Atualiza uma pasta existente.
     * Acesso restrito a 'ADMIN'.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Pasta pasta) {
        try {
            Pasta pastaAtualizada = this.pastaService.atualizar(id, pasta);
            return new ResponseEntity<>(pastaAtualizada, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao atualizar a pasta: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Exclui uma pasta e suas subpastas.
     * Acesso restrito a 'ADMIN'.
     */
    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> excluir(@PathVariable Long id) {
        try {
            this.pastaService.excluir(id);
            return new ResponseEntity<>("Pasta com ID " + id + " excluída com sucesso.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao excluir a pasta: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}