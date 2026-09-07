package com.lagosrent.listing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminReviewListingRequest(
        @NotBlank(message = "Review reason/notes are mandatory")
        String reason,

        @NotNull(message = "Expected listing version for optimistic locking")
        Long expectedVersion
) {
}
