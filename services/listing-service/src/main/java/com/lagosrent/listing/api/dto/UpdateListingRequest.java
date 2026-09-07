package com.lagosrent.listing.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateListingRequest(
        @NotBlank
        String title,
        String description,
        @NotNull @Positive
        BigDecimal rentAmount,
        String rentPeriod,
        @NotNull
        LocalDate availableFrom,
        @Valid
        List<ListingFeeDto> fees
) {
}
