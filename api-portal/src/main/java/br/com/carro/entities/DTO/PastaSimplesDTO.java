package br.com.carro.entities.DTO;

import br.com.carro.entities.Pasta;

public record PastaSimplesDTO(
        Long id,
        String nomePasta,
        String caminhoCompleto
) {
    public PastaSimplesDTO(Pasta pasta) {
        this(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto()
        );
    }
}
