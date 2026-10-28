package com.nutrition.msnutritionplanner.application.receta.dto;

import com.nutrition.msnutritionplanner.domain.receta.Receta;

import java.util.List;

public record RecetaResponse(
        Integer id,
        String nombre,
        String instruccion,
        boolean activo,
        List<Integer> ingredientesIds
) {
    public static RecetaResponse desde(Receta receta) {
        return new RecetaResponse(
                receta.getId(), receta.getNombre(), receta.getInstruccion(),
                receta.isActivo(), receta.getIngredientesIds());
    }
}
