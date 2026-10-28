package com.nutrition.msnutritionplanner.application.receta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecetaRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La instruccion es obligatoria") String instruccion,
        @NotEmpty(message = "Debe indicar al menos un ingrediente") List<Integer> ingredientesIds
) {
}
