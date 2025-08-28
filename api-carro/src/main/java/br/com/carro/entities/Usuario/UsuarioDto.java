package br.com.carro.entities.Usuario;
import br.com.carro.entities.Role.RoleDto;
import java.util.Set;

public record UsuarioDto(Long id, String login, Set<RoleDto> roles) {}