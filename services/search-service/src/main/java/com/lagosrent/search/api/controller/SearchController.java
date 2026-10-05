package com.lagosrent.search.api.controller;

import com.lagosrent.search.api.dto.LocationSuggestionsResponse;
import com.lagosrent.search.api.dto.SearchListingResponse;
import com.lagosrent.search.application.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/listings")
    public ResponseEntity<Page<SearchListingResponse>> searchListings(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String lga,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(searchService.searchListings(
                area, lga, unitType, minRent, maxRent, bedrooms, status, pageable
        ));
    }

    @GetMapping("/listings/{listingId}")
    public ResponseEntity<SearchListingResponse> getListingById(@PathVariable UUID listingId) {
        return ResponseEntity.ok(searchService.getListingById(listingId));
    }

    @GetMapping("/locations")
    public ResponseEntity<LocationSuggestionsResponse> getLocationSuggestions() {
        return ResponseEntity.ok(searchService.getLocationSuggestions());
    }
}
