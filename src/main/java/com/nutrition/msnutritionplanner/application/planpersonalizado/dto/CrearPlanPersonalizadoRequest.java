package com.nutrition.msnutritionplanner.application.planpersonalizado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear un plan personalizado.
 *
 * EXTENSION (Actividad 2): se agrega idPaciente (identificador de BC1, p. ej.
 * "PAC-1024") porque el evento bc2.PlanPersonalizadoCreado lo informa a
 * MsCateringSubscription.
 */
public record CrearPlanPersonalizadoRequest(
        @NotNull(message = "El id del plan de origen es obligatorio") Integer idPlanBase,
        @NotBlank(message = "El id del paciente es obligatorio") String idPaciente,
        @NotBlank(message = "La descripcion de cambios es obligatoria") String descripcionCambios
) {
}
