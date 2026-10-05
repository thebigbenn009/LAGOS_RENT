package com.lagosrent.rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection_requests")
@Getter
@Setter
@NoArgsConstructor
public class InspectionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "tenant_keycloak_id", nullable = false)
    private String tenantKeycloakId;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Column(name = "proposed_datetime", nullable = false)
    private Instant proposedDatetime;

    @Column(name = "confirmed_datetime")
    private Instant confirmedDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionStatus status = InspectionStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public InspectionRequest(UUID listingId, String tenantKeycloakId, String landlordKeycloakId, Instant proposedDatetime, String notes) {
        this.listingId = listingId;
        this.tenantKeycloakId = tenantKeycloakId;
        this.landlordKeycloakId = landlordKeycloakId;
        this.proposedDatetime = proposedDatetime;
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
