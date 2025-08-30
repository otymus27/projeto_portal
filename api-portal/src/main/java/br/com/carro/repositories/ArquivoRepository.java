package br.com.carro.repositories;


import br.com.carro.entities.Arquivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

    Optional<Arquivo> findByNomeArquivo(String nomeArquivo);

    // Método para listar todos os arquivos de uma pasta específica
    List<Arquivo> findByPastaId(Long pastaId);

    // Método para buscar arquivos pelo nome (ou parte dele)
    Page<Arquivo> findByNomeArquivoContainingIgnoreCase(String nomeArquivo, Pageable pageable);

}