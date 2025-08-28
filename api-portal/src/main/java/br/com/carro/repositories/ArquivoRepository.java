package br.com.carro.repositories;

import br.com.carro.entities.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {
    List<Arquivo> findByPastaId(Long pastaId);
}