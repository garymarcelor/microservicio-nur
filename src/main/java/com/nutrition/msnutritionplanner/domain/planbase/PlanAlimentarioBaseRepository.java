package com.nutrition.msnutritionplanner.domain.planbase;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado PlanAlimentarioBase.
 */
public interface PlanAlimentarioBaseRepository {

    PlanAlimentarioBase guardar(PlanAlimentarioBase plan);

    Optional<PlanAlimentarioBase> buscarPorId(Integer id);

    List<PlanAlimentarioBase> listarActivos();

    boolean existePorId(Integer id);
}
