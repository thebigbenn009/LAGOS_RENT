package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String landlordKeycloakId,
        String title,
        String state,
        String lga,
        String area,
        String addressLine1,
        String addressLine2,
        String landmark,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant createdAt
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getLandlordKeycloakId(),
                property.getTitle(),
                property.getState(),
                property.getLga(),
                property.getArea(),
                property.getAddressLine1(),
                property.getAddressLine2(),
                property.getLandmark(),
                property.getLatitude(),
                property.getLongitude(),
                property.getCreatedAt()
        );
    }
}
