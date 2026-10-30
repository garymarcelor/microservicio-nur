package com.nutrition.msnutritionplanner.application.shared.event;

import java.util.Optional;

/**
 * Guarda (por hilo) el correlationId del flujo de negocio en curso.
 *
 * Segun las convenciones del documento de eventos, el correlationId
 * "identifica un flujo de negocio y se propaga sin cambios por todos los
 * eventos del flujo". Se llena desde:
 *  - la cabecera HTTP X-Correlation-Id (filtro de la capa presentation), o
 *  - el sobre de un evento consumido desde Kafka (listeners).
 * Si no hay ninguno, el publicador genera uno por defecto.
 */
public final class CorrelationContext {

    private static final ThreadLocal<String> ACTUAL = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static void establecer(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            ACTUAL.remove();
        } else {
            ACTUAL.set(correlationId);
        }
    }

    public static Optional<String> actual() {
        return Optional.ofNullable(ACTUAL.get());
    }

    /** Debe llamarse al terminar la peticion/mensaje (los hilos se reutilizan). */
    public static void limpiar() {
        ACTUAL.remove();
    }
}
