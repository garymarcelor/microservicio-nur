package com.nutrition.msnutritionplanner.application.receta.dto;

import jakarta.validation.constraints.NotNull;

public record IngredienteRecetaRequest(
        @NotNull(message = "El id del ingrediente es obligatorio") Integer idIngrediente
) {
}
