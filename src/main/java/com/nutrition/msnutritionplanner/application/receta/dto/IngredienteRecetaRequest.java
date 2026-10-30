package com.nutrition.msnutritionplanner.application.receta.dto;

import com.nutrition.msnutritionplanner.domain.receta.IngredienteCantidad;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Ingrediente de una receta con la cantidad por porcion.
 *
 * EXTENSION (Actividad 2): antes solo traia idIngrediente; ahora incluye
 * cantidad y unidad porque el evento bc2.RecetaCreada las exige.
 * Se usa tanto dentro de {@link RecetaRequest} como en el endpoint
 * POST /recetas/{id}/ingredientes.
 */
public record IngredienteRecetaRequest(
        @NotNull(message = "El id del ingrediente es obligatorio") Integer idIngrediente,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero") BigDecimal cantidad,
        @NotNull(message = "La unidad es obligatoria (GRAMOS, MILILITROS, UNIDADES)") UnidadMedida unidad
) {
    /** Convierte el DTO al Value Object de dominio. */
    public IngredienteCantidad aDominio() {
        return new IngredienteCantidad(idIngrediente, cantidad, unidad);
    }
}
