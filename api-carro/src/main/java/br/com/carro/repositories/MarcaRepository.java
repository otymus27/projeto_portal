package br.com.carro.repositories;

import br.com.carro.entities.Marca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarcaRepository extends JpaRepository<Marca, Long> {

    @Override
    Page<Marca> findAll(Pageable pageable);

    // Para busca com filtro por nome (case insensitive)
    Page<Marca> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    // ✅ Método para buscar marcas que contenham a string informada, ignorando maiúsculas e minúsculas.
    // Usado para a tela de busca e para o filtro do relatório.
    List<Marca> findByNomeContainingIgnoreCase(String nome, Sort sort);
}
