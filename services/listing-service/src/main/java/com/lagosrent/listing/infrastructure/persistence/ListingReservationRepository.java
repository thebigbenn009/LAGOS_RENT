package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.ListingReservation;
import com.lagosrent.listing.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListingReservationRepository extends JpaRepository<ListingReservation, UUID> {
    Optional<ListingReservation> findByReservationReference(String reservationReference);

    Optional<ListingReservation> findByListingIdAndStatus(UUID listingId, ReservationStatus status);
}
