package br.com.carro.controllers;

import br.com.carro.entities.Marca;
import br.com.carro.services.MarcaService;
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
@RequestMapping("/api/marca")
public class MarcaController {

    private static final Logger logger = LoggerFactory.getLogger(CarroController.class);
    public record Mensagem(String mensagem) {}

    @Autowired
    private final MarcaService marcaService;

    public MarcaController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    // Listar registros com paginação, filtros e ordenação
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    public ResponseEntity<Page<Marca>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // ✅ Adicione esta linha para verificar os parâmetros que chegam
        System.out.println("Parâmetros de ordenação recebidos: sortField=" + sortField + ", sortDir=" + sortDir);

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Marca> lista;

        if (nome != null && !nome.isBlank()) {
            lista = marcaService.buscarPorNome(nome, pageable);
        } else {
            lista = marcaService.listar(pageable);
        }

        return ResponseEntity.ok(lista);
    }


    // Buscar carro por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            // Chama o service que retorna o carro ou lança exceção se não existir
            Marca marca = marcaService.buscarPorId(id);
            return new ResponseEntity<>(marca, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Cadastrar
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cadastrar(@RequestBody Marca marca) {
        try {
            Marca novaMarca = marcaService.cadastrar(marca);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaMarca);
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
            String msg = marcaService.excluir(id);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao excluir marca: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Atualizar um registro
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> atualizar(@PathVariable Long id, @RequestBody Marca marca) {
        try {
            // Atualiza o carro usando o service; se não existir, lança exceção
            String mensagem = this.marcaService.atualizar(id, marca);
            return new ResponseEntity<>(mensagem, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao atualizar registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/paginado/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Marca>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Page<Marca> marcas = marcaService.listarPaginado(page, size);
        return ResponseEntity.ok(marcas);
    }



}
