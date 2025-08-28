package br.com.carro.controllers;

import br.com.carro.entities.Carro;
import br.com.carro.entities.CarroDTO;
import br.com.carro.entities.Marca;
import br.com.carro.services.CarroService;
import jakarta.transaction.Transactional;
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
import org.slf4j.Logger;


@RestController
@RequestMapping("/api/carro")
public class CarroController {

    private static final Logger logger = LoggerFactory.getLogger(CarroController.class);
    public record Mensagem(String mensagem) {}

    @Autowired
    private final CarroService carroService;

    public CarroController(CarroService carroService) {
        this.carroService = carroService;
    }

    // Listar registros com paginação, filtros e ordenação
    // ✅ Usuários com a role 'USER' ou 'ADMIN' podem acessar este método
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    @GetMapping
    public ResponseEntity<Page<Carro>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String marca, // ✅ Novo parâmetro para o filtro de marca
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Carro> lista;

        if (modelo != null && !modelo.isBlank()) {
            lista = carroService.buscarPorNome(modelo, pageable);
        } else if (ano != null) {
            lista = carroService.buscarPorAno(ano, pageable);
        } else if (marca != null && !marca.isBlank()) { // ✅ Nova condição para filtrar por marca
            lista = carroService.buscarPorMarcaNome(marca, pageable);
        } else {
            lista = carroService.listar(pageable);
        }

        return ResponseEntity.ok(lista);
    }

    // Buscar carro por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            // Chama o service que retorna o objeto ou lança exceção se não existir
            Carro carro = carroService.buscarPorId(id);
            return new ResponseEntity<>(carro, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    public ResponseEntity<String> cadastrar(@RequestBody Carro carro) {
        try {
            String mensagem = this.carroService.cadastrar(carro);
            return new ResponseEntity<>(mensagem, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao cadastrar registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Atualizar um carro
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BASIC','GERENTE')")
    public ResponseEntity<String> atualizar(@PathVariable Long id, @RequestBody Carro carro) {
        try {
            // Atualiza o carro usando o service; se não existir, lança exceção
            String mensagem = this.carroService.atualizar(id, carro);
            return new ResponseEntity<>(mensagem, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao atualizar registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Excluir um carro
    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<String> excluir(@PathVariable Long id) {
        try {
            // Chama o service que já verifica se o carro existe e lança exceção se não existir
            String mensagem = this.carroService.excluir(id);
            return new ResponseEntity<>(mensagem, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao excluir registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


}
