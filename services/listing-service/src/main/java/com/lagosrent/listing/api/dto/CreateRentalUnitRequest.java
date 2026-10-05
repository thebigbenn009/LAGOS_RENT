package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(name = "CreateRentalUnitRequest", description = "Payload for adding an individual rental unit (flat, maisonette, penthouse) to a property")
public record CreateRentalUnitRequest(
        @Schema(description = "Unit door/identifier number", example = "Flat 2B")
        @NotBlank(message = "Unit number is required (e.g. Flat 2B)")
        String unitNumber,

        @Schema(description = "Unit architectural category", example = "TWO_BEDROOM")
        @NotNull(message = "Unit type is required")
        UnitType unitType,

        @Schema(description = "Number of bedrooms", example = "2")
        @NotNull @Positive
        Integer bedrooms,

        @Schema(description = "Number of full bathrooms", example = "2")
        @NotNull @Positive
        Integer bathrooms,

        @Schema(description = "Total number of toilets (including guest powder room)", example = "3")
        @NotNull @Positive
        Integer toilets,

        @Schema(description = "Floor space area in square meters", example = "120.50")
        BigDecimal sizeSqMeters,

        @Schema(description = "Floor level in multi-story building", example = "2")
        Integer floorNumber
) {}
