package br.com.carro.controllers;

import br.com.carro.autenticacao.JpaUserDetailsService;
import br.com.carro.entities.DTO.PastaDTO;
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
    private final JpaUserDetailsService userDetailsService;

    @Autowired
    public PastaController(PastaService pastaService, JpaUserDetailsService userDetailsService) {
        this.pastaService = pastaService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/principais")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Page<PastaDTO>> listarPastasPrincipaisDoUsuario(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "nomePasta") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        if (jwt == null || !jwt.hasClaim("sub")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Usuario usuarioLogado = (Usuario) userDetailsService.loadUserByUsername(jwt.getSubject());

        Sort sortObj = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        boolean isAdmin = usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"));

        return ResponseEntity.ok(pastaService.listarPastasPrincipais(isAdmin, usuarioLogado, pageable));
    }

    @GetMapping("/subpastas/{pastaPaiId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Page<PastaDTO>> listarSubpastas(
            @PathVariable Long pastaPaiId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "nomePasta") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sortObj = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(pastaService.listarSubpastas(pastaPaiId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BASIC', 'GERENTE')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            Pasta pasta = pastaService.buscarPorId(id);
            return new ResponseEntity<>(PastaDTO.fromEntity(pasta), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Cria uma nova pasta. Ela pode ser uma pasta raiz ou uma subpasta.
     * Acesso restrito a 'ADMIN'.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PastaDTO> criarPasta(@RequestBody PastaRequestDTO pastaDTO) {
        try {
            Pasta novaPasta = this.pastaService.criarPasta(pastaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(PastaDTO.fromEntity(novaPasta));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Atualiza uma pasta existente.
     * Acesso restrito a 'ADMIN'.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PastaDTO> atualizar(@PathVariable Long id, @RequestBody Pasta pasta) {
        try {
            Pasta pastaAtualizada = this.pastaService.atualizar(id, pasta);
            return new ResponseEntity<>(PastaDTO.fromEntity(pastaAtualizada), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Exclui uma pasta por ID, incluindo suas subpastas.
     * Acesso restrito a 'ADMIN'.
     */
    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        try {
            this.pastaService.excluir(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}