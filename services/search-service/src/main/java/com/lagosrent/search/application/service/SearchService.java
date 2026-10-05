package com.lagosrent.search.application.service;

import com.lagosrent.search.api.dto.LocationSuggestionsResponse;
import com.lagosrent.search.api.dto.SearchListingResponse;
import com.lagosrent.search.application.exception.ResourceNotFoundException;
import com.lagosrent.search.domain.model.SearchListing;
import com.lagosrent.search.infrastructure.persistence.SearchListingRepository;
import com.lagosrent.search.infrastructure.persistence.SearchSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchListingRepository searchListingRepository;

    @Transactional(readOnly = true)
    public Page<SearchListingResponse> searchListings(
            String area,
            String lga,
            String unitType,
            BigDecimal minRent,
            BigDecimal maxRent,
            Integer bedrooms,
            String status,
            Pageable pageable
    ) {
        Specification<SearchListing> spec = SearchSpecification.filter(area, lga, unitType, minRent, maxRent, bedrooms, status);

        return searchListingRepository.findAll(spec, pageable)
                .map(SearchListingResponse::from);
    }

    @Transactional(readOnly = true)
    public SearchListingResponse getListingById(UUID listingId) {
        SearchListing listing = searchListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("searchListing", listingId));
        return SearchListingResponse.from(listing);
    }

    @Transactional(readOnly = true)
    public LocationSuggestionsResponse getLocationSuggestions() {
        return new LocationSuggestionsResponse(
                searchListingRepository.findDistinctLgas(),
                searchListingRepository.findDistinctAreas()
        );
    }
}
