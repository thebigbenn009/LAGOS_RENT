package com.lagosrent.listing.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AdminReviewListingRequest", description = "Payload for administrative listing review decisions")
public record AdminReviewListingRequest(
        @Schema(description = "Review decision rationale or required modification instructions", example = "Title and photos verified against CAC and deed documents.")
        @NotBlank(message = "Review reason/notes are mandatory")
        String reason,

        @Schema(description = "Expected listing version for optimistic locking concurrency control", example = "1")
        @NotNull(message = "Expected listing version for optimistic locking")
        Long expectedVersion
) {
}
