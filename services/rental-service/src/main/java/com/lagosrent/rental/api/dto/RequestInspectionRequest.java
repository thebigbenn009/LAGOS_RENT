package com.lagosrent.rental.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record RequestInspectionRequest(
        @NotNull(message = "Listing ID is required")
        UUID listingId,

        @NotNull(message = "Proposed inspection datetime is required")
        Instant proposedDatetime,

        String notes
        
) {}
