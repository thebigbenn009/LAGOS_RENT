package com.lagosrent.account.infrastructure.persistence;

import com.lagosrent.account.domain.model.SavedListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedListingRepository extends JpaRepository<SavedListing, UUID> {
    List<SavedListing> findByAccountId(UUID accountId);

    Optional<SavedListing> findByAccountIdAndListingId(UUID accountId, UUID listingId);

    void deleteByAccountIdAndListingId(UUID accountId, UUID listingId);
}
