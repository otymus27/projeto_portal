package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record PastaDTO(
        Long id,
        String nomePasta,
        String caminhoCompleto,
        LocalDateTime dataCriacao,
        Set<PastaDTO> subpastas,
        Set<ArquivoDTO> arquivos
) {
    public static PastaDTO fromEntity(Pasta pasta) {
        Set<PastaDTO> subpastasDto = pasta.getSubpastas() != null ?
                pasta.getSubpastas().stream()
                        .map(PastaDTO::fromEntity)
                        .collect(Collectors.toSet())
                : null;

        Set<ArquivoDTO> arquivosDto = pasta.getArquivos() != null ?
                pasta.getArquivos().stream()
                        .map(ArquivoDTO::fromEntity)
                        .collect(Collectors.toSet())
                : null;

        return new PastaDTO(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                pasta.getDataCriacao(),
                subpastasDto,
                arquivosDto
        );
    }
}