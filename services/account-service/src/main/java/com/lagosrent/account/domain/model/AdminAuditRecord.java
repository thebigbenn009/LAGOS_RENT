package com.lagosrent.account.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_records")
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_keycloak_id", nullable = false)
    private String adminKeycloakId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_type", nullable = false)
    private String entityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "TEXT")
    private String newState;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AdminAuditRecord(String adminKeycloakId, String actionType,
                            String entityType, String reason,
                            String previousState, String newState) {
        this.adminKeycloakId = adminKeycloakId;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = reason;
        this.previousState = previousState;
        this.newState = newState;
        this.createdAt = Instant.now();
    }
}
