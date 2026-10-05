package com.lagosrent.rental.application.service;


import com.lagosrent.rental.domain.model.OutboxEvent;
import com.lagosrent.rental.infrastructure.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves an event to the outbox table within the caller's existing database transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> void recordEvent(String aggregateType, String aggregateId, Long aggregateVersion, String eventType, T payload, String correlationId,
                                String causationId) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                aggregateVersion,
                eventType,
                payloadJson,
                correlationId,
                causationId
        );
        outboxEventRepository.save(outboxEvent);
        log.info("Recorded outbox event: id={}, type={}, aggregateId={}", outboxEvent.getId(), eventType, aggregateId);
    }
}
