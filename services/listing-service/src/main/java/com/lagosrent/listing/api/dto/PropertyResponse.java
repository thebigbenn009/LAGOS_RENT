package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.Property;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PropertyResponse", description = "Property building/compound details")
public record PropertyResponse(
        @Schema(description = "Unique property ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID id,

        @Schema(description = "Keycloak Subject UUID of property owner", example = "landlord-user-123")
        String landlordKeycloakId,

        @Schema(description = "Property title", example = "Silvercrest Court")
        String title,

        @Schema(description = "State", example = "Lagos")
        String state,

        @Schema(description = "Local Government Area", example = "Eti-Osa")
        String lga,

        @Schema(description = "Neighborhood/Area", example = "Lekki Phase 1")
        String area,

        @Schema(description = "Address Line 1", example = "14 Admiralty Way")
        String addressLine1,

        @Schema(description = "Address Line 2", example = "Opposite Lekki Coliseum")
        String addressLine2,

        @Schema(description = "Public landmark", example = "Near Admiralty Toll Gate")
        String landmark,

        @Schema(description = "Latitude coordinate", example = "6.4474")
        BigDecimal latitude,

        @Schema(description = "Longitude coordinate", example = "3.4723")
        BigDecimal longitude,

        @Schema(description = "Record creation timestamp", example = "2026-09-01T10:00:00Z")
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
