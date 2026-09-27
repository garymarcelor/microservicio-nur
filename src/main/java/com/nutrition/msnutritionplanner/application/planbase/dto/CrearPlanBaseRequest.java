package com.nutrition.msnutritionplanner.application.planbase.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CrearPlanBaseRequest(
        @NotNull(message = "La fecha de inicio de vigencia es obligatoria") LocalDate fechaInicioVigencia
) {
}
