package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonBackReference;
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

    /**
     * Relacionamento muitos-para-um com o Setor.
     * Este campo define a qual setor uma pasta principal pertence.
     * Será nulo para subpastas.
     */
    // ✅ Adicione @JsonManagedReference para gerenciar a serialização
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setor_id")
    @JsonManagedReference("setor-pastasPrincipais")
    private Setor setor;

    /**
     * Relacionamento de auto-referência para criar a hierarquia.
     * A 'pastaPai' aponta para a pasta acima dela na árvore.
     */
    // ✅ Adicione @JsonManagedReference para o relacionamento de auto-referência
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pasta_pai_id")
    @JsonManagedReference("pasta-subpastas")
    private Pasta pastaPai;

    /**
     * Lista de subpastas dentro desta pasta.
     * O 'cascade' e 'orphanRemoval' garantem que subpastas sejam deletadas junto com a pasta pai.
     */
    // ✅ Adicione @JsonBackReference para evitar a recursão infinita
    @OneToMany(mappedBy = "pastaPai", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference("pasta-subpastas")
    private List<Pasta> subpastas;

    /**
     * Lista de arquivos contidos nesta pasta.
     * Também usa 'cascade' para gerenciar a exclusão de arquivos quando a pasta é apagada.
     */
    // ✅ Adicione @JsonBackReference para a lista de arquivos
    @OneToMany(mappedBy = "pasta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference("pasta-arquivos")
    private List<Arquivo> arquivos;

    /**
     * Data e hora de criação da pasta.
     */
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    /**
     * Relacionamento muitos-para-muitos para permissões especiais.
     * Permite que usuários de outros setores acessem esta pasta, se necessário.
     */
    @ManyToMany
    @JoinTable(
            name = "tb_permissao_pasta",
            joinColumns = @JoinColumn(name = "pasta_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<Usuario> usuariosComPermissao;
}
