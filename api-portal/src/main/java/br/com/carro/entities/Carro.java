package br.com.carro.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Table(name = "tb_carro")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Carro {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String modelo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "marca_id")
    private Marca marca;

    @Column(nullable = false)
    private String cor;

    @Column(nullable = false)
    private Integer ano;

    @ManyToMany
    @JoinTable(
            name = "tb_carro_proprietario",
            joinColumns = @JoinColumn(name = "carro_id"),
            inverseJoinColumns = @JoinColumn(name = "proprietario_id")
    )

    private List<Proprietario> proprietarios;
}
