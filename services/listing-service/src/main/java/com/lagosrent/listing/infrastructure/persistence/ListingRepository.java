package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.Listing;
import com.lagosrent.listing.domain.model.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByLandlordKeyCloakId(String landlordKeycloakId);

    List<Listing> findByRentalUnit(UUID rentalUnitId);

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
}
