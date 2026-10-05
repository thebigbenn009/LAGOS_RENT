package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.RentalUnit;
import com.lagosrent.listing.domain.model.UnitType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "RentalUnitResponse", description = "Details of a specific rental unit within a property")
public record RentalUnitResponse(
        @Schema(description = "Rental unit ID", example = "550e8400-e29b-41d4-a716-446655440003")
        UUID id,

        @Schema(description = "Parent property ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID propertyId,

        @Schema(description = "Unit door/number designation", example = "Flat 2B")
        String unitNumber,

        @Schema(description = "Unit layout type", example = "TWO_BEDROOM")
        UnitType unitType,

        @Schema(description = "Number of bedrooms", example = "2")
        Integer bedrooms,

        @Schema(description = "Number of full bathrooms", example = "2")
        Integer bathrooms,

        @Schema(description = "Number of toilets", example = "3")
        Integer toilets,

        @Schema(description = "Floor space area in square meters", example = "120.50")
        BigDecimal sizeSqMeters,

        @Schema(description = "Floor level in building", example = "2")
        Integer floorNumber
) {
    public static RentalUnitResponse from(RentalUnit unit) {
        return new RentalUnitResponse(
                unit.getId(),
                unit.getProperty().getId(),
                unit.getUnitNumber(),
                unit.getUnitType(),
                unit.getBedrooms(),
                unit.getBathrooms(),
                unit.getToilets(),
                unit.getSizeSqMeters(),
                unit.getFloorNumber()
        );
    }
}
