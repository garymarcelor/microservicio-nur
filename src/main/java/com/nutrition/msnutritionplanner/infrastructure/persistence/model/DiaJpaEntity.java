package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dia")
@Getter
@Setter
@NoArgsConstructor
public class DiaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dia")
    private Integer id;

    @Column(name = "num_order", nullable = false)
    private Integer numOrder;

    @Column(name = "nro_tiempos_comida")
    private Integer nroTiemposComida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan", nullable = false)
    private PlanAlimentarioJpaEntity plan;

    @OneToMany(mappedBy = "dia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("hora ASC")
    private List<TiempoDeComidaJpaEntity> tiemposDeComida = new ArrayList<>();
}
