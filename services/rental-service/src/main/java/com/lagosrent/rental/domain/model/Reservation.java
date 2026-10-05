package com.lagosrent.rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private RentalApplication application;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "reservation_reference", nullable = false, unique = true)
    private String reservationReference;

    @Column(name = "hold_deadline", nullable = false)
    private Instant holdDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.REQUESTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Reservation(RentalApplication application, UUID listingId, String reservationReference, Instant holdDeadline) {
        this.application = application;
        this.listingId = listingId;
        this.reservationReference = reservationReference;
        this.holdDeadline = holdDeadline;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
