package com.lagosrent.search.infrastructure.persistence;

import com.lagosrent.search.domain.model.SearchListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SearchListingRepository extends JpaRepository<SearchListing, UUID>, JpaSpecificationExecutor<SearchListing> {
    @Query(value = "SELECT DISTINCT area FROM search_listings WHERE area IS NOT NULL ORDER BY area ASC", nativeQuery = true)
    List<String> findDistinctAreas();

    @Query(value = "SELECT DISTINCT lga FROM search_listings WHERE lga IS NOT NULL ORDER BY lga ASC", nativeQuery = true)
    List<String> findDistinctLgas();
}
