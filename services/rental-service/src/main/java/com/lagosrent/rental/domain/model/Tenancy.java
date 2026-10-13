package com.lagosrent.rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static java.time.Instant.now;

@Entity
@Table(name = "tenancies")
@Getter
@Setter
@NoArgsConstructor
public class Tenancy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private RentalApplication application;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "tenant_keycloak_id", nullable = false)
    private String tenantKeycloakId;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Column(name = "annual_rent", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualRent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = now();

    public Tenancy(RentalApplication application, UUID listingId, String tenantKeycloakId, String landlordKeycloakId, BigDecimal annualRent,
                   LocalDate startDate, LocalDate endDate) {
        this.application = application;
        this.listingId = listingId;
        this.tenantKeycloakId = tenantKeycloakId;
        this.landlordKeycloakId = landlordKeycloakId;
        this.annualRent = annualRent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = "ACTIVE";
        this.createdAt = now();
    }
}
