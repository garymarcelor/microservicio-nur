package com.nutrition.msnutritionplanner.application.planbase.dto;

import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;

import java.time.LocalDate;
import java.util.List;

public record PlanBaseResponse(
        Integer id,
        EstadoPlan estadoPlan,
        LocalDate fechaInicioVigencia,
        boolean activo,
        List<DiaResponse> dias
) {
    public static PlanBaseResponse desde(PlanAlimentarioBase plan) {
        return new PlanBaseResponse(
                plan.getId(), plan.getEstadoPlan(), plan.getFechaInicioVigencia(), plan.isActivo(),
                plan.getDias().stream().map(DiaResponse::desde).toList());
    }
}
