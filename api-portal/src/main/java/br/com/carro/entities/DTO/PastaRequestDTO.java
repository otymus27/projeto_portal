package br.com.carro.entities.DTO;

import lombok.Data;

import java.util.Set;

@Data
public class PastaRequestDTO {
    String nomePasta;
    Long pastaPaiId;
    Set<Long> usuariosComPermissaoIds;
}