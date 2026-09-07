package com.lagosrent.listing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_reviews")
@Getter
@Setter
@NoArgsConstructor
public class ListingReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "admin_keycloak_id", nullable = false)
    private String adminKeycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewDecision decision;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "listing_version", nullable = false)
    private Long listingVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ListingReview(Listing listing, String adminKeycloakId, ReviewDecision decision, String reason, Long listingVersion) {
        this.listing = listing;
        this.adminKeycloakId = adminKeycloakId;
        this.decision = decision;
        this.reason = reason;
        this.listingVersion = listingVersion;
        this.createdAt = Instant.now();
    }
}
