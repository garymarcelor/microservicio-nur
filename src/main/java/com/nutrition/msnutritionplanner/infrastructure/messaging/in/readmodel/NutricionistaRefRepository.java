package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio del modelo de lectura de nutricionistas de BC1. */
public interface NutricionistaRefRepository extends JpaRepository<NutricionistaRefJpaEntity, String> {
}
