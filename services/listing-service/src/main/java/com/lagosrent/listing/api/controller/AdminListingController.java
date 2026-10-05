package com.lagosrent.listing.api.controller;

import com.lagosrent.listing.api.dto.AdminReviewListingRequest;
import com.lagosrent.listing.api.dto.ListingResponse;
import com.lagosrent.listing.api.error.ErrorResponse;
import com.lagosrent.listing.application.service.AdminListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin Listings", description = "Administrative review, moderation, and compliance operations for rental listings")
@RestController
@RequestMapping("/api/v1/admin/listings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminListingController {
    private final AdminListingService adminListingService;

    @Operation(
            summary = "Get Review Queue",
            description = "Retrieves paginated listings awaiting administrative compliance review (SUBMITTED status)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review queue retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required")
    })
    @GetMapping("/review-queue")
    public ResponseEntity<Page<ListingResponse>> getReviewQueue(Pageable pageable) {
        return ResponseEntity.ok(adminListingService.getReviewQueue(pageable));
    }

    @Operation(
            summary = "Approve Listing",
            description = "Approves a submitted listing with optimistic concurrency check, enabling the landlord to publish."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing approved successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or version mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/approve")
    public ResponseEntity<ListingResponse> approveListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewListingRequest request
    ) {
        return ResponseEntity.ok(adminListingService.approveListing(
                listingId, jwt.getSubject(), request.reason(), request.expectedVersion()
        ));
    }

    @Operation(
            summary = "Request Changes on Listing",
            description = "Rejects submission back to landlord with mandatory feedback reasons for requested modifications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Changes requested successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or version mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/request-changes")
    public ResponseEntity<ListingResponse> requestChanges(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewListingRequest request
    ) {
        return ResponseEntity.ok(adminListingService.requestChanges(
                listingId, jwt.getSubject(), request.reason(), request.expectedVersion()
        ));
    }

    @Operation(
            summary = "Reject Listing",
            description = "Permanently rejects a submitted listing due to serious policy or verification violations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing rejected successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or version mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/reject")
    public ResponseEntity<ListingResponse> rejectListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewListingRequest request
    ) {
        return ResponseEntity.ok(adminListingService.rejectingListing(
                listingId, jwt.getSubject(), request.reason(), request.expectedVersion()
        ));
    }

    @Operation(
            summary = "Suspend Active Listing",
            description = "Suspends an active published listing due to reports, fraud investigations, or policy violations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing suspended successfully",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or listing not active",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{listingId}/suspend")
    public ResponseEntity<ListingResponse> suspendListing(
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewListingRequest request
    ) {
        return ResponseEntity.ok(adminListingService.suspendListing(
                listingId, jwt.getSubject(), request.reason()
        ));
    }
}
