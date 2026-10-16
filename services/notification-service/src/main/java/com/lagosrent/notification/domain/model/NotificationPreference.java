package com.lagosrent.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_keycloak_id", nullable = false, unique = true)
    private String userKeycloakId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = now();

    public NotificationPreference(String userKeycloakId) {
        this.userKeycloakId = userKeycloakId;
        this.marketingEnabled = false;
        this.updatedAt = now();
    }
}
