package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA unica para ambos agregados de dominio PlanAlimentarioBase
 * y PlanAlimentarioPersonalizado (mapeo por tabla unica con auto-referencia
 * id_plan_base, tal como en el script FOOD_CREATION.sql original):
 * planBase == null  -> es un PlanAlimentarioBase
 * planBase != null  -> es un PlanAlimentarioPersonalizado, derivado de planBase
 */
@Entity
@Table(name = "plan_alimentario")
@Getter
@Setter
@NoArgsConstructor
public class PlanAlimentarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan_base")
    private PlanAlimentarioJpaEntity planBase;

    /**
     * Actividad 2: paciente de BC1 (p. ej. "PAC-1024") al que pertenece un
     * plan personalizado. NULL en planes base. Requerido por el evento
     * bc2.PlanPersonalizadoCreado.
     */
    @Column(name = "id_paciente", length = 40)
    private String idPaciente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_plan")
    private EstadoPlan estadoPlan;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Lob
    @Column(name = "descripcion_cambios")
    private String descripcionCambios;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numOrder ASC")
    private List<DiaJpaEntity> dias = new ArrayList<>();
}
