package com.nutrition.msnutritionplanner.application.ingrediente.dto;

import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;

public record IngredienteResponse(
        Integer id,
        String nombre,
        String descripcion,
        TipoIngrediente tipo,
        boolean activo
) {
    public static IngredienteResponse desde(Ingrediente ingrediente) {
        return new IngredienteResponse(
                ingrediente.getId(),
                ingrediente.getNombre(),
                ingrediente.getDescripcion(),
                ingrediente.getTipo(),
                ingrediente.isActivo());
    }
}
