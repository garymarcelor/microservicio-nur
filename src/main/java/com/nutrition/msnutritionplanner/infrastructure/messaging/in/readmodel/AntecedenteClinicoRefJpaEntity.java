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
 * MODELO DE LECTURA LOCAL de un antecedente clinico alimenticio del paciente
 * (evento bc1.AntecedenteClinicoRegistrado). El nutricionista lo toma en
 * cuenta al crear o ajustar planes (p. ej. alergias o ingredientes restringidos).
 */
@Entity
@Table(name = "antecedente_clinico_ref")
@Getter
@Setter
@NoArgsConstructor
public class AntecedenteClinicoRefJpaEntity {

    /** Id en el contrato, p. ej. "ANT-315". */
    @Id
    @Column(name = "id_antecedente", length = 40)
    private String idAntecedente;

    @Column(name = "id_paciente", nullable = false, length = 40)
    private String idPaciente;

    @Column(name = "tipo", length = 60)
    private String tipo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "severidad", length = 20)
    private String severidad;

    /** Lista de ingredientes restringidos serializada como arreglo JSON (p. ej. ["Maní","Nuez"]). */
    @Column(name = "ingredientes_restringidos", columnDefinition = "TEXT")
    private String ingredientesRestringidos;

    @Column(name = "id_nutricionista", length = 40)
    private String idNutricionista;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;
}
