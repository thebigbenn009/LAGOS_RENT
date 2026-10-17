package com.lagosrent.notification.api.dto;

import com.lagosrent.notification.domain.model.NotificationChannel;
import com.lagosrent.notification.domain.model.NotificationTemplate;

import java.time.Instant;
import java.util.UUID;

public record AdminTemplateResponse(
        UUID id,
        String templateCode,
        NotificationChannel channel,
        String subjectTemplate,
        boolean isHtml,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminTemplateResponse from(NotificationTemplate template) {
        return new AdminTemplateResponse(
                template.getId(),
                template.getTemplateCode(),
                template.getChannel(),
                template.getSubjectTemplate(),
                template.isHtml(),
                template.getVersion(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
