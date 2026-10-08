package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.Listing;
import com.lagosrent.listing.domain.model.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    /**
     * Atomically locks inventory against double-booking race conditions
     * Returns 1 if successfully reserved, 0 if already reserved/rented
     **/
    @Modifying
    @Query("""
                UPDATE Listing l
                SET l.status = com.lagosrent.listing.domain.model.ListingStatus.RESERVED,
                    l.updatedAt = :now
                WHERE l.id = :listingId AND l.status = com.lagosrent.listing.domain.model.ListingStatus.PUBLISHED
            """)
    int atomicallyReserveListing(@Param("listingId") UUID listingId, @Param("now") Instant now);

    /**
     * Compensating action: releases a held listing back to PUBLISHED
     */
    @Modifying
    @Query("""
                UPDATE Listing l
                SET l.status = com.lagosrent.listing.domain.model.ListingStatus.PUBLISHED,
                    l.updatedAt = :now
                WHERE l.id = :listingId AND l.status = com.lagosrent.listing.domain.model.ListingStatus.RESERVED
            """)
    int atomicallyReleaseListing(@Param("listingId") UUID listingId, @Param("now") Instant now);

    /**
     * Finalizes tenancy: marks reserved listing as RENTED.
     */
    @Modifying
    @Query("""
                UPDATE Listing l
                SET l.status = com.lagosrent.listing.domain.model.ListingStatus.RENTED,
                    l.updatedAt = :now
                WHERE l.id = :listingId AND l.status = com.lagosrent.listing.domain.model.ListingStatus.RESERVED
            """)
    int atomicallyMarkRented(@Param("listingId") UUID listingId, @Param("now") Instant now);

    List<Listing> findByLandlordKeycloakId(String landlordKeycloakId);

    List<Listing> findByRentalUnit(UUID rentalUnitId);

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
}
