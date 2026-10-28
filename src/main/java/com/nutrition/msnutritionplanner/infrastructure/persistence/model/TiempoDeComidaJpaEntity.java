package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tiempo_de_comida")
@Getter
@Setter
@NoArgsConstructor
public class TiempoDeComidaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tiempo")
    private Integer id;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dia", nullable = false)
    private DiaJpaEntity dia;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tiempo_de_comida_receta",
            joinColumns = @JoinColumn(name = "id_tiempo"),
            inverseJoinColumns = @JoinColumn(name = "id_receta"))
    private List<RecetaJpaEntity> recetas = new ArrayList<>();
}
