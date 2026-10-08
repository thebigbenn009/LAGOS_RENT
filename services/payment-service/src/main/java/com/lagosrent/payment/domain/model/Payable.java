package com.lagosrent.payment.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.lagosrent.payment.domain.model.PayableStatus.CREATED;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.UUID;
import static java.time.Instant.now;

@Entity
@Table(name = "payables")
@Getter
@Setter
@NoArgsConstructor
public class Payable {
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "tenant_keycloak_id", nullable = false)
    private String tenantKeycloakId;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Column(name = "reservation_reference", nullable = false)
    private String reservationReference;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency = "NGN";

    @Enumerated(STRING)
    @Column(nullable = false)
    private PayableStatus status = CREATED;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = now();

    public Payable(UUID applicationId, UUID listingId, String tenantKeycloakId, String landlordKeycloakId, String reservationReference,
                   BigDecimal totalAmount, String currency, Instant dueDate) {
        this.applicationId = applicationId;
        this.listingId = listingId;
        this.tenantKeycloakId = tenantKeycloakId;
        this.landlordKeycloakId = landlordKeycloakId;
        this.reservationReference = reservationReference;
        this.totalAmount = totalAmount;
        this.currency = currency != null ? currency : "NGN";
        this.dueDate = dueDate;
        this.createdAt = now();
        this.updatedAt = now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = now();
    }
}
