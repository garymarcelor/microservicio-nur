package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * INBOX: registro de los eventos de integracion ya procesados.
 *
 * Kafka garantiza "al menos una vez": un mismo evento puede llegar repetido
 * (reintentos del productor, rebalanceos del consumidor...). Antes de
 * procesar un evento se verifica si su {@code eventId} ya esta aqui; si esta,
 * se descarta. La PK es el eventId, asi que ni siquiera dos hilos
 * simultaneos pueden insertarlo dos veces.
 */
@Entity
@Table(name = "inbox_event")
@Getter
@Setter
@NoArgsConstructor
public class InboxEventJpaEntity {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public InboxEventJpaEntity(String eventId, String eventName, Instant processedAt) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.processedAt = processedAt;
    }
}
