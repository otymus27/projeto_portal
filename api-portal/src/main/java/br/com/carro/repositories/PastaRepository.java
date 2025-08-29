package br.com.carro.repositories;

import br.com.carro.entities.Pasta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface PastaRepository extends JpaRepository<Pasta, Long> {
    /**
     * ✅ Consulta JPQL para buscar pastas principais (aquelas que não têm uma pasta pai) de todos os setores.
     * Esta query é mais robusta e garante que não haverá JOINS indesejados.
     */
    @Query("SELECT p FROM Pasta p WHERE p.pastaPai IS NULL")
    Page<Pasta> findByPastaPaiIsNull(Pageable pageable);

    // Método para buscar subpastas de uma pasta pai específica
    Page<Pasta> findByPastaPaiId(Long pastaPaiId, Pageable pageable);


    Page<Pasta> findAllByIdIn(Set<Long> pastasPrincipaisAcessadas, Pageable pageable);
}