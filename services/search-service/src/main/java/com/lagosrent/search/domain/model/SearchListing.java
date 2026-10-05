package com.lagosrent.search.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "search_listings")
@Getter
@Setter
@NoArgsConstructor
public class SearchListing {

    @Id
    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "listing_version", nullable = false)
    private Long listingVersion;

    @Column(name = "landlord_keycloak_id", nullable = false)
    private String landlordKeycloakId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "rent_period", nullable = false)
    private String rentPeriod;

    @Column(name = "move_in_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal moveInTotal;

    @Column(name = "availableFrom")
    private LocalDate availableFrom;

    @Column(nullable = false)
    private String state = "Lagos";

    @Column(nullable = false)
    private String lga;

    @Column(nullable = false)
    private String area;

    @Column(nullable = false)
    private String landmark;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "unit_type", nullable = false)
    private String unitType;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    @Column(nullable = false)
    private Integer toilets;

    @Column(name = "size_sq_meters", precision = 8, scale = 2)
    private BigDecimal sizeSqMeters;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(nullable = false)
    private String status = "PUBLISHED";

    @Column(name = "primary_image_url")
    private String primaryImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fee_summary", columnDefinition = "jsonb")
    private String feeSummary;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
