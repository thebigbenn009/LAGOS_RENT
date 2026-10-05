package com.lagosrent.listing.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ListingPublishedPayload(
        UUID listingId,
        String landlordKeycloakId,
        UUID propertyId,
        UUID rentalUnitId,
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
        List<FeeItemPayload> fees,
        List<MediaItemPayload> media

) {
    public record FeeItemPayload(
            String feeType,
            BigDecimal amount,
            String recipient,
            String frequency,
            boolean isRefundable,
            String explanation
    ) {}

    public record MediaItemPayload(
            String mediaType,
            String s3Key,
            String filename,
            Integer orderIndex,
            String caption,
            boolean isPrimary
    ) {}
}
