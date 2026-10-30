package com.nutrition.msnutritionplanner.application.shared.event;

import com.nutrition.msnutritionplanner.domain.planbase.PlanAlimentarioBase;
import com.nutrition.msnutritionplanner.domain.planpersonalizado.PlanAlimentarioPersonalizado;
import com.nutrition.msnutritionplanner.domain.receta.Receta;

/**
 * PUERTO DE SALIDA (arquitectura hexagonal / DDD) para los eventos de
 * integracion que MSNutritionPlanner (BC2) publica hacia otros bounded
 * contexts (ver TareaActividad2-tabla-corregida.md, eventos 7, 8 y 9).
 *
 * La capa de aplicacion solo conoce ESTA interfaz; no sabe nada de Kafka,
 * del formato JSON del contrato ni de la tabla outbox. La implementacion
 * vive en la capa de infraestructura
 * ({@code infrastructure.messaging.out.OutboxIntegrationEventPublisher}).
 *
 * REGLA IMPORTANTE (Transactional Outbox): estos metodos deben invocarse
 * DENTRO de la transaccion del caso de uso (@Transactional), despues de
 * persistir el agregado. Asi el cambio de estado y el evento se guardan en
 * MySQL de forma atomica: o se confirman ambos o ninguno.
 */
public interface IntegrationEventPublisher {

    /** bc2.PlanBaseCreado: un plan base quedo COMPLETO y disponible para Catering. */
    void planBaseCreado(PlanAlimentarioBase plan);

    /** bc2.PlanPersonalizadoCreado: un plan personalizado quedo COMPLETO para un paciente. */
    void planPersonalizadoCreado(PlanAlimentarioPersonalizado plan);

    /** bc2.RecetaCreada: nueva receta en el catalogo (la consume produccion, BC4). */
    void recetaCreada(Receta receta);
}
