package com.nutrition.msnutritionplanner.application.planbase.dto;

import com.nutrition.msnutritionplanner.domain.planbase.TiempoDeComida;

import java.time.LocalTime;
import java.util.List;

public record TiempoDeComidaResponse(
        Integer id,
        LocalTime hora,
        List<Integer> recetasIds
) {
    public static TiempoDeComidaResponse desde(TiempoDeComida tiempo) {
        return new TiempoDeComidaResponse(tiempo.getId(), tiempo.getHora().valor(), tiempo.getRecetasIds());
    }
}
