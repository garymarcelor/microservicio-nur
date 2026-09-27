package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receta")
@Getter
@Setter
@NoArgsConstructor
public class RecetaJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_receta")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "instruccion", columnDefinition = "LONGTEXT")
    private String instruccion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "receta_ingrediente",
            joinColumns = @JoinColumn(name = "id_receta"),
            inverseJoinColumns = @JoinColumn(name = "id_ingrediente"))
    private List<IngredienteJpaEntity> ingredientes = new ArrayList<>();
}
