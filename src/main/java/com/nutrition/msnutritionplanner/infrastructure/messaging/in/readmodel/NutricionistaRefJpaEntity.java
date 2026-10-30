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
 * MODELO DE LECTURA LOCAL de un nutricionista de BC1
 * (evento bc1.NutricionistaRegistrado): profesionales habilitados para crear planes.
 */
@Entity
@Table(name = "nutricionista_ref")
@Getter
@Setter
@NoArgsConstructor
public class NutricionistaRefJpaEntity {

    /** Id en el contrato, p. ej. "NUT-27". */
    @Id
    @Column(name = "id_nutricionista", length = 40)
    private String idNutricionista;

    @Column(name = "nombre_completo", length = 200)
    private String nombreCompleto;

    @Column(name = "matricula_profesional", length = 60)
    private String matriculaProfesional;

    @Column(name = "especialidad", length = 120)
    private String especialidad;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_alta")
    private LocalDate fechaAlta;
}
