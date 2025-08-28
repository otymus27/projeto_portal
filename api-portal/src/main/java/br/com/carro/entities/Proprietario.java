package br.com.carro.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Table(name = "tb_proprietario")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Proprietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 15)
    private String telefone;

    @ManyToMany(mappedBy = "proprietarios", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Carro> carros;
}

