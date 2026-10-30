package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio del modelo de lectura de evaluaciones de BC1. */
public interface EvaluacionRefRepository extends JpaRepository<EvaluacionRefJpaEntity, String> {
}
