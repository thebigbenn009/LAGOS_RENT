package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateRentalUnitRequest(
        @NotBlank(message = "Unit number is required (e.g. Flat 2B)")
        String unitNumber,

        @NotNull(message = "Unit type is required")
        UnitType unitType,

        @NotNull @Positive
        Integer bedrooms,

        @NotNull @Positive
        Integer bathrooms,

        @NotNull @Positive
        Integer toilets,

        BigDecimal sizeSqMeters,
        Integer floorNumber
) {}
