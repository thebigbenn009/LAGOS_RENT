package com.lagosrent.search.api.dto;

import com.lagosrent.search.domain.model.SearchListing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SearchListingResponse(
        UUID listingId,
        Long listingVersion,
        String title,
        String description,
        BigDecimal rentAmount,
        String rentPeriod,
        BigDecimal moveInTotal,
        LocalDate availableFrom,
        String state,
        String lga,
        String area,
        String landmark,
        BigDecimal latitude,
        BigDecimal longitude,
        String unitType,
        Integer bedrooms,
        Integer bathrooms,
        Integer toilets,
        BigDecimal sizeSqMeters,
        Integer floorNumber,
        String status,
        String primaryImageUrl,
        Instant publishedAt
) {
    public static SearchListingResponse from(SearchListing entity) {
        return new SearchListingResponse(
                entity.getListingId(),
                entity.getListingVersion(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getRentAmount(),
                entity.getRentPeriod(),
                entity.getMoveInTotal(),
                entity.getAvailableFrom(),
                entity.getState(),
                entity.getLga(),
                entity.getArea(),
                entity.getLandmark(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getUnitType(),
                entity.getBedrooms(),
                entity.getBathrooms(),
                entity.getToilets(),
                entity.getSizeSqMeters(),
                entity.getFloorNumber(),
                entity.getStatus(),
                entity.getPrimaryImageUrl(),
                entity.getPublishedAt()
        );
    }
}
