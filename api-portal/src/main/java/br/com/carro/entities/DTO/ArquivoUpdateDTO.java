package br.com.carro.entities.DTO;

import jakarta.validation.constraints.NotBlank;

public record ArquivoUpdateDTO(
        @NotBlank(message = "O novo nome do arquivo não pode estar em branco.")
        String novoNome
) {}
