package br.com.carro.repositories;

import br.com.carro.entities.Pasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PastaRepository extends JpaRepository<Pasta, Long> {
}
