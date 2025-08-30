package br.com.carro.repositories;

import br.com.carro.entities.Pasta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PastaRepository extends JpaRepository<Pasta, Long> {

    // ✅ Método para carregar a lista de pastas principais com subpastas e arquivos
    @EntityGraph(attributePaths = {"subpastas", "arquivos"})
    Page<Pasta> findByPastaPaiIsNull(Pageable pageable);

    // ✅ Método para carregar a lista de subpastas de uma pasta pai com subpastas e arquivos
    @EntityGraph(attributePaths = {"subpastas", "arquivos"})
    Page<Pasta> findByPastaPaiId(Long pastaPaiId, Pageable pageable);

    // ✅ Método para buscar uma única pasta por ID, carregando todas as suas relações
    @EntityGraph(attributePaths = {"subpastas", "arquivos"})
    Optional<Pasta> findById(Long id);

    // ✅ Método para buscar pastas principais por permissão
    Page<Pasta> findAllByIdIn(Set<Long> pastasPrincipaisAcessadas, Pageable pageable);

    Optional<Pasta> findByNomePastaAndPastaPai(String nomePasta, Pasta pastaPai);

    // Método para buscar arquivos pelo nome (ou parte dele)
    Page<Pasta> findByNomePastaContainingIgnoreCase(String nomePasta, Pageable pageable);

}