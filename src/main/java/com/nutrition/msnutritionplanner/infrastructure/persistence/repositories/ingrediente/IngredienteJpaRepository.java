package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente;

import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredienteJpaRepository extends JpaRepository<IngredienteJpaEntity, Integer> {

    List<IngredienteJpaEntity> findByActivoTrue();

    List<IngredienteJpaEntity> findByIdInAndActivoTrue(List<Integer> ids);

    boolean existsByIdAndActivoTrue(Integer id);
}
