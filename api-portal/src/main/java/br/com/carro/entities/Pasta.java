package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Representa uma pasta no sistema de arquivos.
 * Permite uma estrutura hierárquica (subpastas) e associações a setores e usuários.
 */

@Entity
@Table(name = "tb_pasta")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome da pasta (ex: "Relatorios-2025").
     */
    @Column(name = "nome_pasta", nullable = false)
    private String nomePasta;

    /**
     * Caminho completo da pasta no sistema de arquivos virtual, usado para navegação.
     * Ex: "/financeiro/relatorios/2025/".
     */
    @Column(name = "caminho_completo", nullable = false)
    private String caminhoCompleto;

    // ✅ Vínculo com a pasta pai, para subpastas.
    // Pastas com pastaPai = null são consideradas "pastas principais".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_pai_id")
    @JsonBackReference("pasta-subpastas")
    private Pasta pastaPai;

    /**
     * Lista de subpastas dentro desta pasta.
     * O 'cascade' e 'orphanRemoval' garantem que subpastas sejam deletadas junto com a pasta pai.
     */
    // ✅ Adicione @JsonBackReference para evitar a recursão infinita
    @OneToMany(mappedBy = "pastaPai", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("pasta-subpastas")
    private Set<Pasta> subpastas;

    /**
     * Lista de arquivos contidos nesta pasta.
     * Também usa 'cascade' para gerenciar a exclusão de arquivos quando a pasta é apagada.
     */
    // ✅ Adicione @JsonBackReference para a lista de arquivos
    @OneToMany(mappedBy = "pasta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("pasta-arquivos")
    private Set<Arquivo> arquivos;

    /**
     * Data e hora de criação da pasta.
     */
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    /**
     * Relacionamento muitos-para-muitos para permissões especiais.
     * Permite que usuários de outros setores acessem esta pasta, se necessário.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_permissao_pasta",
            joinColumns = @JoinColumn(name = "pasta_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @JsonIgnore
    private Set<Usuario> usuariosComPermissao;
}
