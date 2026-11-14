package com.lagosrent.payment.infrastructure.messaging;

import com.lagosrent.payment.TestcontainersConfiguration;
import com.lagosrent.payment.domain.model.OutboxEvent;
import com.lagosrent.payment.infrastructure.persistence.OutboxEventRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;


@SpringBootTest(properties = {
        "payment.provider.active=mock",
        "payment.provider.paystack.secret-key=sk_test_mock_secret",
        "payment.checkout.callback-url=http://localhost:8080/callback",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/lagos-rent/protocol/openid-connect/certs",
        "eureka.client.enabled=false"
})
@Import(TestcontainersConfiguration.class)
@DisplayName("Outbox Publisher Asynchronous Dual-Container (PostgreSQL + RabbitMQ) Integration Test")
public class OutboxPublisherIntegrationTest {
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("OutboxPublisher reads pending database event, publishes to RabbitMQ broker, and records published_at")
    void publishPendingEventsDispatchesToRabbitAndMarksPublished() {
        // Given: A pending outbox event in PostgreSQL
        UUID eventId = randomUUID();
        OutboxEvent pendingEvent = new OutboxEvent(
                eventId,
                "Payment",
                randomUUID().toString(),
                1L,
                "payment.succeeded.v1",
                "{\"payableId\":\"" + randomUUID() + "\",\"amount\":350000.00}",
                "CORR-INTEG-9901",
                null
        );
        pendingEvent.setOccurredAt(now());
        outboxEventRepository.saveAndFlush(pendingEvent);

        //  When: The outbox poller runs
        outboxPublisher.publishPendingEvents();

        // Then: Using Awaitility to verify asynchronous database update
        await().atMost(5, SECONDS)
                .pollInterval(ofMillis(100))
                .untilAsserted(() -> {
                    OutboxEvent updated = outboxEventRepository.findById(eventId).orElseThrow();
                    Assertions.assertThat(updated.getPublishedAt()).isNotNull();
                    Assertions.assertThat(updated.getLastError()).isNull();
                });

    }
}
