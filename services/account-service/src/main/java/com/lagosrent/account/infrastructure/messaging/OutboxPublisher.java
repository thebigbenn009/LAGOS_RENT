package com.lagosrent.account.infrastructure.messaging;

import com.lagosrent.account.domain.model.OutboxEvent;
import com.lagosrent.account.infrastructure.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.lagosrent.account.infrastructure.messaging.config.RabbitMqConfig.DOMAIN_EXCHANGE;
import static java.time.Instant.now;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.fetchUnpublishedEvents(BATCH_SIZE);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                JsonNode payloadNode = objectMapper.readTree(event.getPayload());
                EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getAggregateVersion(),
                        event.getOccurredAt(),
                        "account-service",
                        event.getCorrelationId(),
                        event.getCausationId(),
                        payloadNode
                );

                rabbitTemplate.convertAndSend(
                        DOMAIN_EXCHANGE,
                        event.getEventType(),
                        envelope
                );

                event.setPublishedAt(now());
                outboxEventRepository.save(event);
                log.info("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
            }
            catch (Exception ex) {
                event.setAttemptCount(event.getAttemptCount() + 1);
                event.setLastError(ex.getMessage());
                outboxEventRepository.save(event);
                log.error("Failed to publish outbox event: id={}, type={}, error={}",
                        event.getId(), event.getEventType(), ex.getMessage());
            }
        }
    }
}
