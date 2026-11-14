package com.lagosrent.payment.infrastructure.persistence;

import com.lagosrent.payment.TestcontainersConfiguration;
import com.lagosrent.payment.domain.model.OutboxEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("Outbox Event Repository Native PostgreSQL & Lock Integration Test")
public class OutboxEventRepositoryIntegrationTest {
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("fetchUnpublishedEvents returns only records where published_at is NULL in chronological order")
    void fetchUnpublishedEventsOrdersCorrectlyAndExcludesPublished() {
        Instant t1 = now().minusSeconds(60);
        Instant t2 = now().minusSeconds(30);
        Instant t3 = now();

        // 1. First event: unpublished
        OutboxEvent event1 = new OutboxEvent(
                randomUUID(), "Payment", randomUUID().toString(), 1L,
                "payment.initiated.v1", "{\"ref\":\"PAY-01\"}", "CORR-01", null
        );
        event1.setOccurredAt(t1);

        // 2. Second event: Already Published
        OutboxEvent event2 = new OutboxEvent(
                randomUUID(), "Payment", randomUUID().toString(), 1L,
                "payment.succeeded.v1", "{\"ref\":\"PAY-02\"}", "CORR-02", null
        );
        event2.setOccurredAt(t2);
        event2.setPublishedAt(now());

        // 3. Third event: unpublished
        OutboxEvent event3 = new OutboxEvent(
                randomUUID(), "Payment", randomUUID().toString(), 1L,
                "payment.succeeded.v1", "{\"ref\":\"PAY-03\"}", "CORR-03", null
        );
        event3.setOccurredAt(t3);

        outboxEventRepository.saveAllAndFlush(List.of(event1, event2, event3));

        // When: Querying unpublished events with batch size 10
        List<OutboxEvent> unpublished = outboxEventRepository.fetchUnpublishedEvents(10);

        // Then: Exactly 2 events returned (event2 is excluded), ordered t1 before t3
        assertThat(unpublished).hasSize(2);
        Assertions.assertThat(unpublished.get(0).getId()).isEqualTo(event1.getId());
        Assertions.assertThat(unpublished.get(1).getId()).isEqualTo(event3.getId());
    }
}
