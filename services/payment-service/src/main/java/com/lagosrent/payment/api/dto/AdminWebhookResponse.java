package com.lagosrent.payment.api.dto;

import com.lagosrent.payment.domain.model.ProviderWebhook;
import com.lagosrent.payment.domain.model.WebhookStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminWebhookResponse(
        UUID id,
        String providerName,
        String eventType,
        String providerReference,
        WebhookStatus status,
        String processingError,
        Instant receivedAt,
        Instant processedAt
) {
    public static AdminWebhookResponse from(ProviderWebhook webhook) {
        return new AdminWebhookResponse(
                webhook.getId(),
                webhook.getProviderName(),
                webhook.getEventType(),
                webhook.getProviderReference(),
                webhook.getStatus(),
                webhook.getProcessingError(),
                webhook.getReceivedAt(),
                webhook.getProcessedAt()
        );
    }
}
