package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Representa um arquivo PDF no sistema.
 * Armazena metadados do arquivo, mas não o seu conteúdo binário.
 */

@Entity
@Table(name = "tb_arquivo")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome do arquivo PDF (ex: "relatorio_final.pdf").
     */
    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    /**
     * Caminho real do arquivo PDF no sistema de arquivos do servidor.
     */
    @Column(name = "caminho_armazenamento", nullable = false)
    private String caminhoArmazenamento;

    /**
     * Tamanho do arquivo PDF em bytes.
     */
    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    /**
     * Data e hora em que o arquivo foi enviado.
     */
    @Column(name = "data_upload", nullable = false)
    private LocalDateTime dataUpload;

    /**
     * Relacionamento muitos-para-um. O arquivo PDF pertence a uma Pasta.
     */
    @ManyToOne
    @JoinColumn(name = "pasta_id")
    @JsonBackReference("pasta-arquivos")
    private Pasta pasta;

    /**
     * Relacionamento muitos-para-um. O usuário que enviou o arquivo.
     */
    @ManyToOne
    @JoinColumn(name = "criado_por_id")
    private Usuario criadoPor;
}