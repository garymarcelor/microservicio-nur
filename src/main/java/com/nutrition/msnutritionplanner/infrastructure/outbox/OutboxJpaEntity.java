package com.nutrition.msnutritionplanner.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * TRANSACTIONAL OUTBOX - fila de la tabla {@code outbox_event} (MySQL).
 *
 * Cada evento de integracion se guarda aqui, en la MISMA transaccion que el
 * cambio de negocio (p. ej. completar un plan). Un proceso aparte
 * ({@link OutboxRelay}) lee las filas pendientes y las publica en Kafka.
 * Asi nunca ocurre "guarde en BD pero no pude avisar a Kafka" (ni al reves).
 *
 * Una fila esta PENDIENTE mientras {@code publishedAt} sea null.
 */
@Entity
@Table(name = "outbox_event", indexes = {
        // El relay busca siempre "pendientes, en orden de insercion"
        @Index(name = "idx_outbox_pendientes", columnList = "published_at, id")
})
@Getter
@NoArgsConstructor
public class OutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID del evento (el mismo "eventId" del sobre). Unico: evita duplicar filas. */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    /** Nombre del evento, p. ej. "bc2.PlanBaseCreado". Viaja como cabecera Kafka "eventName". */
    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    /** Topic de destino. */
    @Column(name = "topic", nullable = false, length = 150)
    private String topic;

    /**
     * Key del mensaje Kafka (id del agregado, p. ej. "PLAN-41"): todos los
     * eventos del mismo agregado van a la misma particion y se mantienen en orden.
     */
    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    /** Sobre completo en JSON (eventId, eventName, eventVersion, occurredOn, source, correlationId, data). */
    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Momento en que Kafka confirmo la recepcion. null = pendiente. */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** Cantidad de intentos fallidos de publicacion. */
    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    public OutboxJpaEntity(String eventId, String eventName, String topic, String messageKey,
                           String payload, Instant createdAt) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    /** Kafka confirmo el mensaje: la fila deja de estar pendiente. */
    public void marcarPublicado(Instant cuando) {
        this.publishedAt = cuando;
        this.ultimoError = null;
    }

    /** La publicacion fallo: se cuenta el intento y se guarda el motivo; se reintentara. */
    public void registrarFallo(String error) {
        this.intentos++;
        this.ultimoError = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }
}
