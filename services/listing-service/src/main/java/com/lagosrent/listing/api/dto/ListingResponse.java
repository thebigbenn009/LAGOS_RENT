package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.Listing;
import com.lagosrent.listing.domain.model.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID rentalUnitId,
        String landlordKeycloakId,
        String title,
        String description,
        ListingStatus status,
        BigDecimal rentAmount,
        String rentPeriod,
        BigDecimal moveInTotal,
        LocalDate availableFrom,
        String state,
        String lga,
        String area,
        String landmark,
        Long version,
        List<ListingFeeDto> fees,
        List<ListingMediaDto> media,
        Instant createdAt,
        Instant updatedAt
) {
    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getRentalUnit().getId(),
                listing.getLandlordKeycloakId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getStatus(),
                listing.getRentAmount(),
                listing.getRentPeriod(),
                listing.getMoveInTotal(),
                listing.getAvailableFrom(),
                listing.getRentalUnit().getProperty().getState(),
                listing.getRentalUnit().getProperty().getLga(),
                listing.getRentalUnit().getProperty().getArea(),
                listing.getRentalUnit().getProperty().getLandmark(),
                listing.getVersion(),
                listing.getFees().stream().map(ListingFeeDto::from).toList(),
                listing.getMedia().stream().map(ListingMediaDto::from).toList(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()

        );
    }
}
