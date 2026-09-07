package com.lagosrent.listing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReserveListingRequest(
        @NotBlank(message = "Reservation refrence is required")
        String reservationReference,
        @NotNull(message = "Hold until timestamp is required")
        Instant heldUntil
) {}
