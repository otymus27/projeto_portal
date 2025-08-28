package br.com.carro.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Table(name = "tb_marca")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Marca {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;

    //Relacionamento com Carro - uma marca pode estar vinculado a vários carros
    @OneToMany(mappedBy = "marca")
    @JsonBackReference("marca-carros")
    private List<Carro> carros;

}
