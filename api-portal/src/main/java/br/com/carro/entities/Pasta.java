package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "tb_pasta")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome_pasta", nullable = false)
    private String nomePasta;

    @Column(name = "caminho_completo", nullable = false)
    private String caminhoCompleto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_pai_id")
    @JsonIgnore
    private Pasta pastaPai;

    @OneToMany(mappedBy = "pastaPai", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Pasta> subpastas;

    @OneToMany(mappedBy = "pasta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Arquivo> arquivos;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_permissao_pasta",
            joinColumns = @JoinColumn(name = "pasta_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @JsonIgnore
    private Set<Usuario> usuariosComPermissao;
}