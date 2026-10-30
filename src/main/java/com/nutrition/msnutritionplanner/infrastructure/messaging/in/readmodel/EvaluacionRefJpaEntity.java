package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MODELO DE LECTURA LOCAL de una evaluacion realizada a un paciente
 * (evento bc1.EvaluacionRealizada). Si {@code requiereAjustePlan} es true,
 * BC2 debe ajustar el plan del paciente.
 */
@Entity
@Table(name = "evaluacion_ref")
@Getter
@Setter
@NoArgsConstructor
public class EvaluacionRefJpaEntity {

    /** Id en el contrato, p. ej. "EVA-912". */
    @Id
    @Column(name = "id_evaluacion", length = 40)
    private String idEvaluacion;

    @Column(name = "id_paciente", nullable = false, length = 40)
    private String idPaciente;

    @Column(name = "id_suscripcion", length = 40)
    private String idSuscripcion;

    @Column(name = "id_nutricionista", length = 40)
    private String idNutricionista;

    @Column(name = "tipo_evaluacion", length = 60)
    private String tipoEvaluacion;

    @Column(name = "fecha_evaluacion")
    private LocalDate fechaEvaluacion;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "imc", precision = 5, scale = 2)
    private BigDecimal imc;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "requiere_ajuste_plan", nullable = false)
    private boolean requiereAjustePlan;
}
