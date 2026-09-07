package com.lagosrent.listing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePropertyRequest(
        @NotBlank(message = "Property title is required")
        @Size(max = 255)
        String title,

        @NotBlank(message = "LGA is required")
        String lga,

        @NotBlank(message = "Area/Neighborhood is required")
        String area,

        @NotBlank(message = "Address Line 1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "Landmark is required for privacy-safe public search")
        String landmark,

        BigDecimal latitude,
        BigDecimal longitude
) {
}
