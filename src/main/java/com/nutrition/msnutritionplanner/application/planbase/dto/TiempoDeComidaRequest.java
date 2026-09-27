package com.nutrition.msnutritionplanner.application.planbase.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record TiempoDeComidaRequest(
        @NotNull(message = "La hora es obligatoria") LocalTime hora,
        @NotEmpty(message = "Debe indicar al menos una receta") List<Integer> recetasIds
) {
}
