package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio del modelo de lectura de pacientes de BC1. */
public interface PacienteRefRepository extends JpaRepository<PacienteRefJpaEntity, String> {
}
