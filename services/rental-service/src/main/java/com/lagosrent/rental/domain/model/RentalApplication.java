package com.lagosrent.rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rental_applications")
@Getter
@Setter
@NoArgsConstructor
public class RentalApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "tenant_keycloak_id", nullable = false)
    private String tenantKeycloakId;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "previous_landlord_contact")
    private String previousLandlordContact;

    @OneToOne(mappedBy = "application", cascade = CascadeType.ALL)
    private PriceSnapshot priceSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public RentalApplication(UUID listingId, String tenantKeycloakId, String landlordKeycloakId) {
        this.listingId = listingId;
        this.tenantKeycloakId = tenantKeycloakId;
        this.landlordKeycloakId = landlordKeycloakId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

}
