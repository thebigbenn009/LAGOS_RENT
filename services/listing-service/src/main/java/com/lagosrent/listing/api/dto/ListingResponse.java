package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.Listing;
import com.lagosrent.listing.domain.model.ListingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(name = "ListingResponse", description = "Complete listing details including status, pricing, location, and fee breakdown")
public record ListingResponse(
        @Schema(description = "Listing Unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Associated Rental Unit ID", example = "550e8400-e29b-41d4-a716-446655440003")
        UUID rentalUnitId,

        @Schema(description = "Keycloak Subject UUID of Landlord", example = "landlord-user-123")
        String landlordKeycloakId,

        @Schema(description = "Listing Title", example = "Luxury 2-Bedroom Flat in Lekki Phase 1")
        String title,

        @Schema(description = "Detailed property and amenity description", example = "Modern apartment with 24/7 power, swimming pool, and fitted kitchen.")
        String description,

        @Schema(description = "Listing Lifecycle Status", example = "PUBLISHED")
        ListingStatus status,

        @Schema(description = "Base Rent in NGN", example = "3500000.00")
        BigDecimal rentAmount,

        @Schema(description = "Rent Payment Frequency", example = "YEARLY")
        String rentPeriod,

        @Schema(description = "Total Move-in Cost in NGN", example = "4200000.00")
        BigDecimal moveInTotal,

        @Schema(description = "Date from which unit is available for move-in", example = "2026-11-01")
        LocalDate availableFrom,

        @Schema(description = "State where property is situated", example = "Lagos")
        String state,

        @Schema(description = "Local Government Area", example = "Eti-Osa")
        String lga,

        @Schema(description = "Area or Neighborhood", example = "Lekki Phase 1")
        String area,

        @Schema(description = "Privacy-safe public landmark", example = "Near Admiralty Toll Gate")
        String landmark,

        @Schema(description = "Optimistic Locking Version", example = "1")
        Long version,

        @Schema(description = "Breakdown of mandatory & optional fees")
        List<ListingFeeDto> fees,

        @Schema(description = "Listing media assets (photos, floor plans, virtual tours)")
        List<ListingMediaDto> media,

        @Schema(description = "Listing creation timestamp", example = "2026-09-01T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Listing last update timestamp", example = "2026-09-02T15:30:00Z")
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
