package com.lagosrent.account.infrastructure.messaging;

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
}
