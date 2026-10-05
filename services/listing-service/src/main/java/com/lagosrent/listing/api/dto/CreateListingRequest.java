package com.lagosrent.listing.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "CreateListingRequest", description = "Payload for creating a rental listing draft for a rental unit")
public record CreateListingRequest(
        @Schema(description = "Listing public headline", example = "Luxury 2-Bedroom Flat in Lekki Phase 1")
        @NotBlank(message = "Listing title is required")
        String title,

        @Schema(description = "Detailed property marketing and amenity description", example = "Modern apartment with 24/7 power, swimming pool, and fitted kitchen.")
        String description,

        @Schema(description = "Base rent in NGN", example = "3500000.00")
        @NotNull @Positive(message = "Base rent amount is required")
        BigDecimal rentAmount,

        @Schema(description = "Rent frequency period (e.g. YEARLY, MONTHLY)", example = "YEARLY")
        String rentPeriod,

        @Schema(description = "Date from which unit is ready for occupancy", example = "2026-11-01")
        @NotNull(message = "Available from date is required")
        LocalDate availableFrom,

        @Schema(description = "Mandatory and optional fee breakdown items (caution deposit, service charge, etc.)")
        @Valid
        List<ListingFeeDto> fees
) {
}
