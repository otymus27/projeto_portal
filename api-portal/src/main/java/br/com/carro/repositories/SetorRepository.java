package br.com.carro.repositories;

import br.com.carro.entities.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    // Método para buscar um setor pelo nome, ignorando maiúsculas/minúsculas
    Page<Setor> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}