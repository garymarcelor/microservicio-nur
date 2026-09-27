package com.nutrition.msnutritionplanner.application.planbase.dto;

import com.nutrition.msnutritionplanner.domain.planbase.Dia;

import java.util.List;

public record DiaResponse(
        Integer id,
        int numOrden,
        int nroTiemposComida,
        List<TiempoDeComidaResponse> tiemposDeComida
) {
    public static DiaResponse desde(Dia dia) {
        return new DiaResponse(
                dia.getId(),
                dia.getNumOrden().valor(),
                dia.getNroTiemposComida(),
                dia.getTiemposDeComida().stream().map(TiempoDeComidaResponse::desde).toList());
    }
}
