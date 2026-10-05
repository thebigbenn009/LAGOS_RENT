package com.lagosrent.search.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_messages")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedMessage {

    @EmbeddedId
    private ProcessedMessageId id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedMessage(UUID eventId, String consumerName, String eventType) {
        this.id = new ProcessedMessageId(eventId, consumerName);
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class ProcessedMessageId implements Serializable {
        @Column(name = "event_id", nullable = false)
        private UUID eventId;

        @Column(name = "consumer_name", nullable = false)
        private String consumerName;

        public ProcessedMessageId(UUID eventId, String consumerName) {
            this.eventId = eventId;
            this.consumerName = consumerName;
        }
    }
}
