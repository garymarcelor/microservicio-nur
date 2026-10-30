package com.nutrition.msnutritionplanner.application.planpersonalizado.dto;

import com.nutrition.msnutritionplanner.application.planbase.dto.DiaResponse;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.shared.EstadoPlan;

import java.time.LocalDate;
import java.util.List;

public record PlanPersonalizadoResponse(
        Integer id,
        Integer idPlanBase,
        String idPaciente,
        String descripcionCambios,
        EstadoPlan estadoPlan,
        LocalDate fechaInicioVigencia,
        boolean activo,
        List<DiaResponse> dias
) {
    public static PlanPersonalizadoResponse desde(PlanAlimentarioPersonalizado plan) {
        return new PlanPersonalizadoResponse(
                plan.getId(), plan.getIdPlanBase(), plan.getIdPaciente(), plan.getDescripcionCambios(),
                plan.getEstadoPlan(), plan.getFechaInicioVigencia(), plan.isActivo(),
                plan.getDias().stream().map(DiaResponse::desde).toList());
    }
}
