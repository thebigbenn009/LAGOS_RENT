package com.lagosrent.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import static com.lagosrent.notification.domain.model.DeliveryStatus.PENDING;
import static com.lagosrent.notification.domain.model.NotificationChannel.EMAIL;
import static jakarta.persistence.EnumType.STRING;
import static java.time.Instant.now;

@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
@NoArgsConstructor
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(STRING)
    @Column(nullable = false)
    private NotificationChannel channel = EMAIL;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(STRING)
    @Column(nullable = false)
    private DeliveryStatus status = PENDING;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = now();

    public NotificationDelivery(UUID eventId, String templateCode, String recipient, NotificationChannel channel, String subject, String body) {
        this.eventId = eventId;
        this.templateCode = templateCode;
        this.recipient = recipient;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.attemptCount = 1;
        this.createdAt = now();

    }
}
