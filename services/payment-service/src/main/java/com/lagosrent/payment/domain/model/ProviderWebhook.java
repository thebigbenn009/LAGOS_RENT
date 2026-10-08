package com.lagosrent.payment.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;
import java.util.UUID;

import static com.lagosrent.payment.domain.model.WebhookStatus.RECEIVED;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.UUID;
import static java.time.Instant.now;
import static org.hibernate.type.SqlTypes.JSON;


@Entity
@Table(name = "provider_webhooks")
@Getter
@Setter
@NoArgsConstructor
public class ProviderWebhook {
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "provider_reference")
    private String providerReference;

    @JdbcTypeCode(JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "signature_header", columnDefinition = "TEXT")
    private String signatureHeader;

    @Enumerated(STRING)
    @Column(nullable = false)
    private WebhookStatus status = RECEIVED;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = now();

    @Column(name = "processed_at")
    private Instant processedAt;

    public ProviderWebhook(String providerName, String eventType, String providerReference, String payload, String signatureHeader) {
        this.providerName = providerName;
        this.eventType = eventType;
        this.providerReference = providerReference;
        this.payload = payload;
        this.signatureHeader = signatureHeader;
        this.receivedAt = now();
    }
}
