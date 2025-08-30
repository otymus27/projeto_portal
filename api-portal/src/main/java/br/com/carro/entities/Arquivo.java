package br.com.carro.entities;

import br.com.carro.entities.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_arquivo")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "caminho_armazenamento", nullable = false)
    private String caminhoArmazenamento;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "data_upload", nullable = false)
    private LocalDateTime dataUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pasta_id")
    @JsonIgnore
    private Pasta pasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id")
    @JsonIgnore
    private Usuario criadoPor;
}