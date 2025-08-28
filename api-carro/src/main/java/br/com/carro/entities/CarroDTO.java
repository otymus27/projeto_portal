package br.com.carro.entities;

import java.util.List;

public record CarroDTO(
        Long id,
        String modelo,
        String cor,
        Integer ano,
        Marca marca,
        List<Proprietario> proprietarios
) {}
