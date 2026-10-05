package com.lagosrent.listing.api.controller;

import com.lagosrent.listing.api.dto.CreateListingRequest;
import com.lagosrent.listing.api.dto.ListingMediaDto;
import com.lagosrent.listing.api.dto.ListingResponse;
import com.lagosrent.listing.api.dto.UpdateListingRequest;
import com.lagosrent.listing.api.error.ErrorResponse;
import com.lagosrent.listing.application.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Listings", description = "Public & Landlord operations for rental listings")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
public class ListingController {
    private final ListingService listingService;

    @Operation(
            summary = "Create a New Listing",
            description = "Creates a draft listing for an existing rental unit under the authenticated landlord's ownership."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Listing draft created successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid unit",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — User does not own the unit's property",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/units/{unitId}/listings")
    public ResponseEntity<ListingResponse> createListing(
            @Parameter(description = "Rental unit ID", example = "550e8400-e29b-41d4-a716-446655440003")
            @PathVariable UUID unitId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateListingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.createListing(unitId, jwt.getSubject(), request));
    }

    @Operation(
            summary = "Update an Existing Listing",
            description = "Updates pricing, descriptions, and fee breakdown for a draft or modification-requested listing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing updated successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or listing not editable in current status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — User is not the owner of this listing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/listings/{listingId}")
    public ResponseEntity<ListingResponse> updateListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        return ResponseEntity.ok(listingService.updateListing(listingId, jwt.getSubject(), request));
    }

    @Operation(
            summary = "Add Media to Listing",
            description = "Uploads and attaches a photo, video tour, or floor plan asset to a listing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Media attached successfully",
                    content = @Content(schema = @Schema(implementation = ListingMediaDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid media metadata",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — User does not own this listing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/listings/{listingId}/media")
    public ResponseEntity<ListingMediaDto> addMedia(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ListingMediaDto mediaDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.addMedia(listingId, jwt.getSubject(), mediaDto));
    }

    @Operation(
            summary = "Submit Listing for Review",
            description = "Submits a draft listing into the administrative compliance review queue."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing submitted for review successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Listing is missing required details or media",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — User does not own this listing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/listings/{listingId}/submit")
    public ResponseEntity<ListingResponse> submitListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(listingService.submitForReview(listingId, jwt.getSubject()));
    }

    @Operation(
            summary = "Publish Listing",
            description = "Publishes an approved listing, making it active and publicly searchable across the Lagos Rent platform."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing published successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Listing is not in APPROVED state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — User does not own this listing",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/listings/{listingId}/publish")
    public ResponseEntity<ListingResponse> publishListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(listingService.publishListing(listingId, jwt.getSubject()));
    }

    @Operation(
            summary = "Get Listing Details by ID",
            description = "Returns complete listing details including status, rent pricing, and all mandatory/optional fee breakdowns."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing found",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/listings/{listingId}")
    public ResponseEntity<ListingResponse> getListingById(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId
    ) {
        return ResponseEntity.ok(listingService.getListingById(listingId));
    }

    @Operation(
            summary = "Get Landlord Listings",
            description = "Retrieves all listings belonging to the authenticated landlord across all statuses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Landlord listings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/listings/my-listings")
    public ResponseEntity<List<ListingResponse>> getMyListings(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(listingService.getLandlordListings(jwt.getSubject()));
    }
}
