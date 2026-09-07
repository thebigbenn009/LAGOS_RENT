package com.lagosrent.listing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_unit_id", nullable = false)
    private RentalUnit rentalUnit;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "rent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "rent_period", nullable = false)
    private String rentPeriod = "ANNUALLY";

    @Column(name = "move_in_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal moveInTotal = BigDecimal.ZERO;

    @Column(name = "available_from", nullable = false)
    private LocalDate availableFrom;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingFee> fees = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingMedia> media = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Listing(RentalUnit rentalUnit, String landlordKeycloakId, String title, String description, BigDecimal rentAmount, LocalDate availableFrom) {
        this.rentalUnit = rentalUnit;
        this.landlordKeycloakId = landlordKeycloakId;
        this.title = title;
        this.description = description;
        this.rentAmount = rentAmount;
        this.availableFrom = availableFrom;
        this.moveInTotal = rentAmount;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
