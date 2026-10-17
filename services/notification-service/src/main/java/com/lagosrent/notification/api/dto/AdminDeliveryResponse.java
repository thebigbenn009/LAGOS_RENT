package com.lagosrent.notification.api.dto;

import com.lagosrent.notification.domain.model.DeliveryStatus;
import com.lagosrent.notification.domain.model.NotificationChannel;
import com.lagosrent.notification.domain.model.NotificationDelivery;

import java.time.Instant;
import java.util.UUID;

public record AdminDeliveryResponse(
        UUID id,
        UUID eventId,
        String templateCode,
        String recipient,
        NotificationChannel channel,
        String subject,
        DeliveryStatus status,
        String providerName,
        String providerMessageId,
        String errorMessage,
        int attemptCount,
        Instant sentAt,
        Instant createdAt
) {
    public static AdminDeliveryResponse from(NotificationDelivery delivery) {
        return new AdminDeliveryResponse(
                delivery.getId(),
                delivery.getEventId(),
                delivery.getTemplateCode(),
                delivery.getRecipient(),
                delivery.getChannel(),
                delivery.getSubject(),
                delivery.getStatus(),
                delivery.getProviderName(),
                delivery.getProviderMessageId(),
                delivery.getErrorMessage(),
                delivery.getAttemptCount(),
                delivery.getSentAt(),
                delivery.getCreatedAt()
        );
    }
}
