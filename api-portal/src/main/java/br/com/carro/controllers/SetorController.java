package br.com.carro.controllers;

import br.com.carro.entities.Setor;
import br.com.carro.services.SetorService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/setor")
public class SetorController {

    private static final Logger logger = LoggerFactory.getLogger(SetorController.class);
    public record Mensagem(String mensagem) {}

    @Autowired
    private final SetorService setorService;

    public SetorController(SetorService setorService) {
        this.setorService = setorService;
    }

    // Listar registros com paginação, filtros e ordenação
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    public ResponseEntity<Page<Setor>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Setor> lista;

        if (nome != null && !nome.isBlank()) {
            lista = setorService.buscarPorNome(nome, pageable);
        } else {
            lista = setorService.listar(pageable);
        }

        return ResponseEntity.ok(lista);
    }


    // Buscar setor por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            Setor setor = setorService.buscarPorId(id);
            return new ResponseEntity<>(setor, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Cadastrar
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cadastrar(@RequestBody Setor setor) {
        try {
            Setor novoSetor = setorService.cadastrar(setor);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoSetor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar registro: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> excluir(@PathVariable Long id) {
        try {
            String msg = setorService.excluir(id);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao excluir setor: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Atualizar um registro
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> atualizar(@PathVariable Long id, @RequestBody Setor setor) {
        try {
            String mensagem = this.setorService.atualizar(id, setor);
            return new ResponseEntity<>(mensagem, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao atualizar registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/paginado/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Setor>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Page<Setor> setores = setorService.listarPaginado(page, size);
        return ResponseEntity.ok(setores);
    }
}