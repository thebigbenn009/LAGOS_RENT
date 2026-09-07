package com.lagosrent.listing.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateListingRequest(
        @NotBlank(message = "Listing title is required")
        String title,

        String description,

        @NotNull @Positive(message = "Base rent amount is required")
        BigDecimal rentAmount,

        String period,

        @NotNull(message = "Available from date is required")
        LocalDate availableFrom,

        @Valid
        List<ListingFeeDto> fees
) {
}
