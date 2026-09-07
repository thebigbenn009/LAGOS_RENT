package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.RentalUnit;
import com.lagosrent.listing.domain.model.UnitType;

import java.math.BigDecimal;
import java.util.UUID;

public record RentalUnitResponse(
        UUID id,
        UUID propertyId,
        String unitNumber,
        UnitType unitType,
        Integer bedrooms,
        Integer bathrooms,
        Integer toilets,
        BigDecimal sizeSqMeters,
        Integer floorNumber
) {
    public static RentalUnitResponse from(RentalUnit unit) {
        return new RentalUnitResponse(
                unit.getId(),
                unit.getProperty().getId(),
                unit.getUnitNumber(),
                unit.getUnitType(),
                unit.getBathrooms(),
                unit.getBedrooms(),
                unit.getToilets(),
                unit.getSizeSqMeters(),
                unit.getFloorNumber()
        );
    }
}
