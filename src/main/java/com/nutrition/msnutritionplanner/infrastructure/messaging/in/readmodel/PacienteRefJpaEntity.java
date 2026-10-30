package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * MODELO DE LECTURA LOCAL de un paciente de BC1 (MsPatientsConsulCore).
 *
 * BC2 no consulta a BC1 por HTTP: guarda aqui una copia minima de lo que BC1
 * le cuenta por eventos (bc1.PacienteRegistrado, bc1.EvaluacionRealizada,
 * bc1.ProximaEvaluacionProgramada). Asi puede validar/usar datos del paciente
 * aunque BC1 este caido (autonomia de los microservicios).
 *
 * Es una "referencia", no un agregado de BC2: la verdad vive en BC1.
 * Si un evento llega antes que bc1.PacienteRegistrado (orden no garantizado
 * entre topics), se crea la fila solo con el id y luego se completa.
 */
@Entity
@Table(name = "paciente_ref")
@Getter
@Setter
@NoArgsConstructor
public class PacienteRefJpaEntity {

    /** Id del paciente en el contrato, p. ej. "PAC-1024". */
    @Id
    @Column(name = "id_paciente", length = 40)
    private String idPaciente;

    @Column(name = "nombre_completo", length = 200)
    private String nombreCompleto;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "sexo", length = 5)
    private String sexo;

    @Column(name = "telefono", length = 40)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "id_nutricionista", length = 40)
    private String idNutricionista;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    // --- bc1.ProximaEvaluacionProgramada ---
    @Column(name = "proxima_evaluacion_fecha")
    private LocalDate proximaEvaluacionFecha;

    @Column(name = "proxima_evaluacion_tipo", length = 60)
    private String proximaEvaluacionTipo;

    @Column(name = "requiere_analisis_clinico")
    private Boolean requiereAnalisisClinico;

    // --- bc1.EvaluacionRealizada ---
    /** true si la ultima evaluacion pidio ajustar el plan del paciente (queda pendiente para el nutricionista). */
    @Column(name = "ajuste_plan_pendiente", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private boolean ajustePlanPendiente;

    @Column(name = "ultima_evaluacion_id", length = 40)
    private String ultimaEvaluacionId;

    public PacienteRefJpaEntity(String idPaciente) {
        this.idPaciente = idPaciente;
    }
}
