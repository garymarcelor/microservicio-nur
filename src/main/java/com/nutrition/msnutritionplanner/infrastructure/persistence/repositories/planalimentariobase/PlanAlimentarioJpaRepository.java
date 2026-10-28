package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.planalimentariobase;

import com.nutrition.msnutritionplanner.infrastructure.persistence.model.PlanAlimentarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data para la tabla unica plan_alimentario, compartida
 * por los agregados PlanAlimentarioBase (planBase == null) y
 * PlanAlimentarioPersonalizado (planBase != null).
 */
public interface PlanAlimentarioJpaRepository extends JpaRepository<PlanAlimentarioJpaEntity, Integer> {

    List<PlanAlimentarioJpaEntity> findByActivoTrueAndPlanBaseIsNull();

    List<PlanAlimentarioJpaEntity> findByActivoTrueAndPlanBaseIsNotNull();

    List<PlanAlimentarioJpaEntity> findByActivoTrueAndPlanBase_Id(Integer idPlanBase);

    boolean existsByIdAndActivoTrueAndPlanBaseIsNull(Integer id);

    boolean existsByIdAndActivoTrueAndPlanBaseIsNotNull(Integer id);
}
