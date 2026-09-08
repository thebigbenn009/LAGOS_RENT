package com.lagosrent.listing.application.service;

import com.lagosrent.listing.api.dto.ReserveListingRequest;
import com.lagosrent.listing.application.exception.ResourceNotFoundException;
import com.lagosrent.listing.domain.model.Listing;
import com.lagosrent.listing.domain.model.ListingReservation;
import com.lagosrent.listing.domain.model.ListingStatus;
import com.lagosrent.listing.domain.model.ReservationStatus;
import com.lagosrent.listing.infrastructure.persistence.ListingRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingReservationService {
    private final ListingRepository listingRepository;
    private final ListingReservationRepository reservationRepository;

    @Transactional
    public void reserveListing(UUID listingId, ReserveListingRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED listings can be reserved. Current status: " + listing.getStatus());
        }

        listing.setStatus(ListingStatus.RESERVED);
        listingRepository.save(listing);

        ListingReservation reservation = new ListingReservation(
                listing,
                request.reservationReference(),
                request.heldUntil()
        );
        reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseReservation(UUID listingId, String reservationReference) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        ListingReservation reservation = reservationRepository.findByReservationReference(reservationReference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationReference));

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        if (listing.getStatus() == ListingStatus.RESERVED) {
            listing.setStatus(ListingStatus.PUBLISHED);
            listingRepository.save(listing);
        }
    }
}
