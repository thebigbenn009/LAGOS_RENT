package com.lagosrent.rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rental_sagas")
@Getter
@Setter
@RequiredArgsConstructor
public class RentalSaga {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private RentalApplication application;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "reservation_reference")
    private String reservationReference;

    @Column(name = "current_step", nullable = false)
    private String currentStep = "RESERVATION_REQUESTED";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public RentalSaga(RentalApplication application, UUID listingId, String reservationReference) {
        this.application = application;
        this.listingId = listingId;
        this.reservationReference = reservationReference;
        this.currentStep = "RESERVATION_REQUESTED";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
