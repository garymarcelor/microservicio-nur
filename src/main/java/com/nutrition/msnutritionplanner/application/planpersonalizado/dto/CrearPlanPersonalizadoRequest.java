package com.nutrition.msnutritionplanner.application.planpersonalizado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearPlanPersonalizadoRequest(
        @NotNull(message = "El id del plan de origen es obligatorio") Integer idPlanBase,
        @NotBlank(message = "La descripcion de cambios es obligatoria") String descripcionCambios
) {
}
