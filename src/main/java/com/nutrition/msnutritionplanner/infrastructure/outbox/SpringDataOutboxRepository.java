package com.nutrition.msnutritionplanner.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio Spring Data de la tabla outbox_event.
 *
 * La consulta de pendientes usa {@code FOR UPDATE SKIP LOCKED} (MySQL 8+):
 * bloquea las filas leidas y las que ya estan bloqueadas por otra replica del
 * microservicio se saltan, de modo que dos instancias nunca publican la
 * misma fila a la vez. Debe ejecutarse dentro de una transaccion.
 */
public interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, Long> {

    @Query(value = "SELECT * FROM outbox_event WHERE published_at IS NULL "
            + "ORDER BY id LIMIT :limite FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxJpaEntity> bloquearPendientes(@Param("limite") int limite);

    /** Retencion: borra las filas ya publicadas anteriores a la fecha dada. */
    @Modifying
    @Query("DELETE FROM OutboxJpaEntity o WHERE o.publishedAt IS NOT NULL AND o.publishedAt < :limite")
    int borrarPublicadosAntesDe(@Param("limite") Instant limite);

    /** Cantidad de eventos aun sin publicar (util para pruebas y monitoreo). */
    long countByPublishedAtIsNull();
}
