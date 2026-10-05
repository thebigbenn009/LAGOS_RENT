package com.lagosrent.rental.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateApplicationRequest(
        @NotNull(message = "Listing ID is required")
        UUID listingId,

        String employmentStatus,
        BigDecimal annualIncome,
        String previousLandlordContact
) {
}
