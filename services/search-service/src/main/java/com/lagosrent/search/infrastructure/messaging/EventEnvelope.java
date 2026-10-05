package com.lagosrent.search.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Long aggregateVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        String causationId,
        T payload
) {
    public static <T> EventEnvelope<T> of(
            String eventType,
            String aggregateType,
            String aggregateId,
            Long aggregateVersion,
            String producer,
            String correlationId,
            String causationId,
            T payload
    ) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                aggregateVersion,
                Instant.now(),
                producer,
                correlationId,
                causationId,
                payload
        );
    }
    
}
