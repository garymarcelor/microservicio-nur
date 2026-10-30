package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    /**
     * Actividad 2: peso de una porcion en gramos (viaja en bc2.RecetaCreada
     * y en las recetas de bc2.PlanBaseCreado / bc2.PlanPersonalizadoCreado).
     * El DEFAULT permite agregar la columna a una base con datos existentes.
     */
    @Column(name = "porcion_gramos", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 100")
    private Integer porcionGramos;

    /**
     * Actividad 2: antes era un @ManyToMany directo hacia Ingrediente; ahora
     * la tabla puente es una entidad con cantidad y unidad por porcion.
     * orphanRemoval elimina la fila puente cuando se quita un ingrediente.
     */
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RecetaIngredienteJpaEntity> ingredientes = new ArrayList<>();
}
