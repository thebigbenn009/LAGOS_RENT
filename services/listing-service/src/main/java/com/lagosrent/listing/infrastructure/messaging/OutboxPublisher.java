package com.lagosrent.listing.infrastructure.messaging;

import com.lagosrent.listing.domain.model.OutboxEvent;
import com.lagosrent.listing.infrastructure.messaging.config.RabbitMqConfig;
import com.lagosrent.listing.infrastructure.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 500) // Polls every 500ms
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.fetchUnpublishedEvents(BATCH_SIZE);
        if (pendingEvents.isEmpty()) {
            return;
        }
        for (OutboxEvent event : pendingEvents) {
            try {
                // Construct the event envelope
                JsonNode payloadNode = objectMapper.readTree(event.getPayload());
                EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getAggregateId(),
                        event.getAggregateVersion(),
                        event.getOccurredAt(),
                        "listing-service",
                        event.getCorrelationId(),
                        event.getCausationId(),
                        payloadNode
                );

                // Publish to RabbitMQ Topic Exchange using the event_type as the routing key
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.DOMAIN_EXCHANGE,
                        event.getEventType(),
                        envelope
                );

                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                log.info("Published outbox event to RabbitMQ: id={}, type={}, routingKey={}",
                        event.getId(), event.getEventType(), event.getEventType());
            }
            catch (Exception ex) {
                event.setAttemptCount(event.getAttemptCount() + 1);
                event.setLastError(ex.getMessage());
                log.error("Failed to publish outbox event id={}: {}", event.getId(), ex.getMessage(), ex);
            }
        }

        outboxEventRepository.saveAll(pendingEvents);
    }
}
