package com.nutrition.msnutritionplanner.infrastructure.messaging.in.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio del inbox (deduplicacion de eventos consumidos por eventId). */
public interface InboxEventRepository extends JpaRepository<InboxEventJpaEntity, String> {
}
