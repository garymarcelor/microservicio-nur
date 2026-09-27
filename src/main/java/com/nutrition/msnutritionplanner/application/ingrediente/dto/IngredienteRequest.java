package com.nutrition.msnutritionplanner.application.ingrediente.dto;

import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngredienteRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String descripcion,
        @NotNull(message = "El tipo de ingrediente es obligatorio") TipoIngrediente tipo
) {
}
