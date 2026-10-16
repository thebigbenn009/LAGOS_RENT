package com.lagosrent.account.application.service;

import com.lagosrent.account.domain.model.OutboxEvent;
import com.lagosrent.account.infrastructure.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static java.util.UUID.randomUUID;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves an event to the outbox table within the caller's existing database transaction.
     */
    @Transactional(propagation = MANDATORY)
    public void recordEvent(
            String aggregateType,
            String aggregateId,
            Long aggregateVersion,
            String eventType,
            Object payload,
            String correlationId,
            String causationId
    ) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        OutboxEvent event = new OutboxEvent(
                randomUUID(),
                aggregateType,
                aggregateId,
                aggregateVersion,
                eventType,
                payloadJson,
                correlationId,
                causationId
        );
        outboxEventRepository.save(event);
        log.info("Recorded outbox event: type={}, aggregateId={}", eventType, aggregateId);
    }
}
