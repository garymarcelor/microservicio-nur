package com.nutrition.msnutritionplanner.application.receta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Datos para crear/actualizar una receta.
 *
 * EXTENSION (Actividad 2): se agrega porcionGramos y se reemplaza la lista
 * plana de ingredientesIds por ingredientes con cantidad y unidad, datos
 * reales que viajan en el evento bc2.RecetaCreada hacia MsProductionPackaging.
 */
public record RecetaRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La instruccion es obligatoria") String instruccion,
        @NotNull(message = "La porcion en gramos es obligatoria")
        @Positive(message = "La porcion en gramos debe ser mayor a cero") Integer porcionGramos,
        @NotEmpty(message = "Debe indicar al menos un ingrediente") List<@Valid IngredienteRecetaRequest> ingredientes
) {
    /** Ids de los ingredientes indicados (para validar su existencia). */
    public List<Integer> ingredientesIds() {
        return ingredientes.stream().map(IngredienteRecetaRequest::idIngrediente).toList();
    }
}
