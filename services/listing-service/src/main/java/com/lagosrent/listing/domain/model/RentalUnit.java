package com.lagosrent.listing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rental_units", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"property_id", "unit_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class RentalUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Column(nullable = false)
    private Integer bedrooms = 1;

    @Column(nullable = false)
    private Integer bathrooms = 1;

    @Column(nullable = false)
    private Integer toilets = 1;

    @Column(name = "size_sq_meters", precision = 8, scale = 2)
    private BigDecimal sizeSqMeters;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public RentalUnit(Property property, String unitNumber, UnitType unitType, Integer bedrooms, Integer bathrooms, Integer toilets) {
        this.property = property;
        this.unitNumber = unitNumber;
        this.unitType = unitType;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.toilets = toilets;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
