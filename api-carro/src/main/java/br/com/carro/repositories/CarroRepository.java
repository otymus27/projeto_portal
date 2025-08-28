package br.com.carro.repositories;

import br.com.carro.entities.Carro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarroRepository extends JpaRepository<Carro,Long> {

    @Override
    Page<Carro> findAll(Pageable pageable);

    // Para busca com filtro por modelo (case insensitive)
    Page<Carro> findByModeloContainingIgnoreCase(String modelo, Pageable pageable);

    Page<Carro> findByAno(Integer ano, Pageable pageable);

    Page<Carro> findByMarcaNomeContainingIgnoreCase(String nomeMarca, Pageable pageable);



}
