package br.com.carro.entities.DTO;

import br.com.carro.entities.Arquivo;
import java.time.LocalDateTime;

public record ArquivoDTO(
        Long id,
        String nomeArquivo,
        String caminhoArmazenamento,
        Long tamanhoBytes,
        LocalDateTime dataUpload
) {
    public static ArquivoDTO fromEntity(Arquivo arquivo) {
        return new ArquivoDTO(
                arquivo.getId(),
                arquivo.getNomeArquivo(),
                arquivo.getCaminhoArmazenamento(),
                arquivo.getTamanhoBytes(),
                arquivo.getDataUpload()
        );
    }
}