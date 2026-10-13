package com.lagosrent.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import static com.lagosrent.notification.domain.model.NotificationChannel.EMAIL;
import static jakarta.persistence.EnumType.STRING;
import static java.time.Instant.now;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_code", nullable = false, unique = true)
    private String templateCode;

    @Enumerated(STRING)
    @Column(nullable = false)
    private NotificationChannel channel = EMAIL;

    @Column(name = "subject_template", nullable = false, columnDefinition = "TEXT")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;


    @Column(name = "is_html", nullable = false)
    private boolean isHtml = true;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = now();
    }
}
