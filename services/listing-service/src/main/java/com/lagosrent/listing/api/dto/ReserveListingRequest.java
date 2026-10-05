package com.lagosrent.listing.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(name = "ReserveListingRequest", description = "Payload for placing a temporary hold/reservation on a listing")
public record ReserveListingRequest(
        @Schema(description = "Unique correlation/reservation reference from rental service", example = "RES-2026-98124")
        @NotBlank(message = "Reservation refrence is required")
        String reservationReference,

        @Schema(description = "Expiration timestamp until which listing is reserved", example = "2026-10-04T16:00:00Z")
        @NotNull(message = "Hold until timestamp is required")
        Instant heldUntil
) {}
