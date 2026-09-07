package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.ListingFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingFeeRepository extends JpaRepository<ListingFee, UUID> {
    List<ListingFee> findByListingId(UUID listingId);
}
