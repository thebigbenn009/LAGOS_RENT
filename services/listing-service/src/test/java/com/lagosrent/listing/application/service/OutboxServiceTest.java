package com.lagosrent.listing.application.service;

import com.lagosrent.listing.domain.model.OutboxEvent;
import com.lagosrent.listing.infrastructure.messaging.payload.ListingReservedPayload;
import com.lagosrent.listing.infrastructure.persistence.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService - Transactional Outbox Recording Unit Tests")
public class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("serializes payload to JSON and saves an unpublished Outbox event with aggregate and trace metadata")
    void serializesPayloadAndSavesUnpublishedOutboxEvent() {
        // given
        UUID listingId = randomUUID();
        Instant heldUntil = now().plus(24, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        Instant reservedAt = now().truncatedTo(ChronoUnit.MILLIS);
        ListingReservedPayload payload = new ListingReservedPayload(
                listingId,
                "RES-2026-0042",
                heldUntil,
                reservedAt
        );

        // When
        outboxService.recordEvent(
                "listing",
                listingId.toString(),
                3L,
                "listing.reserved.v1",
                payload,
                "corr-7b91",
                "cause-1a04"
        );

        // then
        then(outboxEventRepository).should().save(outboxEventCaptor.capture());
        OutboxEvent saved = outboxEventCaptor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("listing");
        assertThat(saved.getAggregateId()).isEqualTo(listingId.toString());
        assertThat(saved.getAggregateVersion()).isEqualTo(3L);
        assertThat(saved.getEventType()).isEqualTo("listing.reserved.v1");
        assertThat(saved.getCorrelationId()).isEqualTo("corr-7b91");
        assertThat(saved.getCausationId()).isEqualTo("cause-1a04");
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getOccurredAt()).isNotNull();

        JsonNode json = objectMapper.readTree(saved.getPayload());
        assertThat(json.path("listingId").asString()).isEqualTo(listingId.toString());
        assertThat(json.path("reservationReference").asString()).isEqualTo("RES-2026-0042");
    }

    @Test
    @DisplayName("generates a distinct eventId for each recorded event so consumers can deduplicate by eventId")
    void generatesDistinctEventIdPerCall() {
        // given
        UUID listingId = randomUUID();
        ListingReservedPayload payload = new ListingReservedPayload(
                listingId,
                "RES-2026-0042",
                now().plus(24, ChronoUnit.HOURS),
                now()
        );

        // When
        outboxService.recordEvent("listing", listingId.toString(), 1L, "listing.reserved.v1", payload, null, null);
        outboxService.recordEvent("listing", listingId.toString(), 2L, "listing.reserved.v1", payload, null, null);

        // then
        then(outboxEventRepository).should(times(2)).save(outboxEventCaptor.capture());
        List<OutboxEvent> captured = outboxEventCaptor.getAllValues();

        assertThat(captured.get(0).getId())
                .isNotNull()
                .isNotEqualTo(captured.get(1).getId());
    }

    @Test
    @DisplayName("propagates serialization error and does not save to repository when ObjectMapper fails")
    void doesNotSaveWhenSerializationFails() {
        // given
        Object unserializablePayload = new Object() {
            public String getBrokenProperty() {
                throw new IllegalStateException("Simulated serialization failure");
            }
        };

        // when / then
        assertThatThrownBy(() -> outboxService.recordEvent(
                "listing",
                UUID.randomUUID().toString(),
                1L,
                "listing.published.v1",
                unserializablePayload,
                null,
                null
        )).isInstanceOf(JacksonException.class);

        then(outboxEventRepository).shouldHaveNoInteractions();
    }
}
