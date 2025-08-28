package br.com.carro.entities.Usuario;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
public record UsuarioCadastroDto(
        @NotBlank
        String login,
        @NotBlank
        String senha,
        Set<Long> roleIds
) {
}
