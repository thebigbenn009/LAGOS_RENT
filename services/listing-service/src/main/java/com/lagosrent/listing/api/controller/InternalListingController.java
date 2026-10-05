package com.lagosrent.listing.api.controller;

import com.lagosrent.listing.api.dto.ReserveListingRequest;
import com.lagosrent.listing.api.error.ErrorResponse;
import com.lagosrent.listing.application.service.ListingReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Internal Listings", description = "Internal inter-service operations for reservations and tenancy coordination")
@RestController
@RequestMapping("/internal/v1/listings")
@RequiredArgsConstructor
public class InternalListingController {

    private final ListingReservationService reservationService;

    @Operation(
            summary = "Reserve Listing",
            description = "Places a temporary hold/reservation on a listing for an active tenant rental application."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing reserved successfully"),
            @ApiResponse(responseCode = "400", description = "Listing cannot be reserved or invalid state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/reserve")
    public ResponseEntity<Void> reserveListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Valid @RequestBody ReserveListingRequest request) {
        reservationService.reserveListing(listingId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Release Reservation",
            description = "Releases an active temporary reservation and restores listing to PUBLISHED status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation released successfully"),
            @ApiResponse(responseCode = "400", description = "Reservation reference mismatch or invalid state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/release")
    public ResponseEntity<Void> releaseReservation(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(description = "Reservation reference from rental-service", example = "RES-2026-98124")
            @RequestParam String reservationReference
    ) {
        reservationService.releaseReservation(listingId, reservationReference);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Confirm Tenancy (Rent)",
            description = "Confirms lease tenancy upon payment receipt and transitions listing status to RENTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenancy confirmed and listing marked as RENTED"),
            @ApiResponse(responseCode = "400", description = "Reservation reference mismatch or invalid state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/rent")
    public ResponseEntity<Void> confirmTenancy(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(description = "Reservation reference from rental-service", example = "RES-2026-98124")
            @RequestParam String reservationReference
    ) {
        reservationService.confirmTenancy(listingId, reservationReference);
        return ResponseEntity.ok().build();
    }
}
