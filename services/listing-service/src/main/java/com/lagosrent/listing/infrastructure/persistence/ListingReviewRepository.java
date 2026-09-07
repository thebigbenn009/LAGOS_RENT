package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.ListingReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingReviewRepository extends JpaRepository<ListingReview, UUID> {
    List<ListingReview> findByListingIdOrderByCreatedAtDesc(UUID listingId);
}
