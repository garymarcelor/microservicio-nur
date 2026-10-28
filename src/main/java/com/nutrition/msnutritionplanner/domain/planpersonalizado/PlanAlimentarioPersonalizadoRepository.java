package com.nutrition.msnutritionplanner.domain.planpersonalizado;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado PlanAlimentarioPersonalizado.
 */
public interface PlanAlimentarioPersonalizadoRepository {

    PlanAlimentarioPersonalizado guardar(PlanAlimentarioPersonalizado plan);

    Optional<PlanAlimentarioPersonalizado> buscarPorId(Integer id);

    List<PlanAlimentarioPersonalizado> listarActivosPorPlanBase(Integer idPlanBase);

    boolean existePorId(Integer id);
}
