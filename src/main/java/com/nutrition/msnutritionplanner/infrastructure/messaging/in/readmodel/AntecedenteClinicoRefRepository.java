package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio del modelo de lectura de antecedentes clinicos de BC1. */
public interface AntecedenteClinicoRefRepository extends JpaRepository<AntecedenteClinicoRefJpaEntity, String> {
}
