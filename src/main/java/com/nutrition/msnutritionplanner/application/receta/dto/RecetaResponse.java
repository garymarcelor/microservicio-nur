package com.nutrition.msnutritionplanner.application.receta.dto;

import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta REST de una receta.
 *
 * EXTENSION (Actividad 2): se agrega porcionGramos y el detalle de
 * ingredientes (cantidad y unidad). Se conserva ingredientesIds para no
 * romper a los clientes existentes.
 */
public record RecetaResponse(
        Integer id,
        String nombre,
        String instruccion,
        Integer porcionGramos,
        boolean activo,
        List<Integer> ingredientesIds,
        List<IngredienteDetalle> ingredientes
) {
    /** Ingrediente de la receta con su cantidad por porcion. */
    public record IngredienteDetalle(Integer idIngrediente, BigDecimal cantidad, UnidadMedida unidad) {
    }

    public static RecetaResponse desde(Receta receta) {
        return new RecetaResponse(
                receta.getId(), receta.getNombre(), receta.getInstruccion(), receta.getPorcionGramos(),
                receta.isActivo(), receta.getIngredientesIds(),
                receta.getIngredientes().stream()
                        .map(i -> new IngredienteDetalle(i.idIngrediente(), i.cantidad(), i.unidad()))
                        .toList());
    }
}
