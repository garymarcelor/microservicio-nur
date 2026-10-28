package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaJpaRepository extends JpaRepository<RecetaJpaEntity, Integer> {

    List<RecetaJpaEntity> findByActivoTrue();

    List<RecetaJpaEntity> findByIdInAndActivoTrue(List<Integer> ids);

    boolean existsByIdAndActivoTrue(Integer id);
}
