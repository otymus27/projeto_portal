package br.com.carro.repositories;

import br.com.carro.entities.Pasta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PastaRepository extends JpaRepository<Pasta, Long> {

    // Método para buscar pastas principais (aquelas que não têm uma pasta pai) de todos os setores
    Page<Pasta> findByPastaPaiIsNull(Pageable pageable);

    // Método para buscar pastas principais de um setor específico
    Page<Pasta> findBySetorIdAndPastaPaiIsNull(Long setorId, Pageable pageable);

    // Método para buscar subpastas de uma pasta pai específica
    Page<Pasta> findByPastaPaiId(Long pastaPaiId, Pageable pageable);



}