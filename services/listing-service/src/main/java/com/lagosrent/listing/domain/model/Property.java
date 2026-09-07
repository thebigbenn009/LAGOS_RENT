package com.lagosrent.listing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String state = "Lagos";

    @Column(nullable = false)
    private String lga;

    @Column(nullable = false)
    private String area;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2", nullable = false)
    private String addressLine2;

    @Column(nullable = false)
    private String landmark;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Property(String landlordKeycloakId, String title, String lga, String area, String addressLine1, String landmark) {
        this.landlordKeycloakId = landlordKeycloakId;
        this.title = title;
        this.state = "Lagos";
        this.lga = lga;
        this.area = area;
        this.addressLine1 = addressLine1;
        this.landmark = landmark;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
