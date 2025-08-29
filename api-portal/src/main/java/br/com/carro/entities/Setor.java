package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * Representa um setor ou departamento da empresa.
 * Cada setor pode ter múltiplos usuários e pastas principais.
 */

@Entity
@Table(name = "tb_setor")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Setor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome do setor. Deve ser único para evitar duplicidade.
     */
    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    /**
     * Relacionamento um-para-muitos com a entidade Usuario.
     * Um Setor pode ter muitos Usuarios. O 'mappedBy' indica que o relacionamento
     * é gerenciado pelo campo 'setor' na entidade Usuario.
     */
    @OneToMany(mappedBy = "setor")
    @JsonBackReference("setor-usuarios") // ✅ Já ignora a serialização ou recursividade infinita
    private Set<Usuario> usuarios;

    /**
     * Relacionamento um-para-muitos com a entidade Pasta.
     * Um Setor pode ter muitas Pastas que são consideradas 'principais' ou raiz.
     */
    @OneToMany(mappedBy = "setor")
    @JsonBackReference("setor-pastasPrincipais") // ✅ Já ignora a serialização ou recursividade infinita
    private Set<Pasta> pastasPrincipais;
}