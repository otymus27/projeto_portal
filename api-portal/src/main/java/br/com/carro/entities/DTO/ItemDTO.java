package br.com.carro.entities.DTO;

import java.util.List;

// Novo DTO para a estrutura hierárquica
public record ItemDTO(
        String nome,
        boolean isDiretorio,
        List<ItemDTO> filhos,
        double tamanho,
        Integer contagem
) {}
