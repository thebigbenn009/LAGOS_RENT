package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.ListingMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingMediaRepository extends JpaRepository<ListingMedia, UUID> {
    List<ListingMedia> findByListingIdOrderByOrderIndexAsc(UUID listingId);
}
