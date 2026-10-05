package com.lagosrent.account.api.controller;

import com.lagosrent.account.api.dto.*;
import com.lagosrent.account.api.error.ErrorResponse;
import com.lagosrent.account.application.service.AccountService;
import com.lagosrent.account.application.service.VerificationService;
import com.lagosrent.account.domain.model.Account;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts & Profiles", description = "Current user account, tenant/landlord profiles, KYC document verification, and saved listings operations")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final VerificationService verificationService;

    @Operation(
            summary = "Get Current Account",
            description = "Retrieves or auto-provisions the authenticated user's account details synced from Keycloak JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getCurrentAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        Account account = accountService.getOrCreateAccountFromJwt(jwt);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @Operation(
            summary = "Update Tenant Profile",
            description = "Updates tenant rental preferences, budget, occupation, and desired location."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant profile updated successfully",
                    content = @Content(schema = @Schema(implementation = TenantProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/me/tenant-profile")
    public ResponseEntity<TenantProfileResponse> updateTenantProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateTenantProfileRequest request
    ) {
        return ResponseEntity.ok(accountService.updateTenantProfile(jwt.getSubject(), request));
    }

    @Operation(
            summary = "Get Tenant Profile",
            description = "Retrieves current user's tenant profile preferences."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TenantProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Tenant profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me/tenant-profile")
    public ResponseEntity<TenantProfileResponse> getTenantProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(accountService.getTenantProfile(jwt.getSubject()));
    }

    @Operation(
            summary = "Update Landlord Profile",
            description = "Updates landlord business name and bank payout disbursement information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Landlord profile updated successfully",
                    content = @Content(schema = @Schema(implementation = LandlordProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/me/landlord-profile")
    public ResponseEntity<LandlordProfileResponse> updateLandlordProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateLandlordProfileRequest request
    ) {
        return ResponseEntity.ok(accountService.updateLandlordProfile(jwt.getSubject(), request));
    }

    @Operation(
            summary = "Get Landlord Profile",
            description = "Retrieves landlord profile, payout bank details, and verification status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Landlord profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LandlordProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Landlord profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me/landlord-profile")
    public ResponseEntity<LandlordProfileResponse> getLandlordProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(accountService.getLandlordProfile(jwt.getSubject()));
    }

    @Operation(
            summary = "Submit KYC Verification Document",
            description = "Submits an identity or ownership document for admin landlord verification review."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification case submitted successfully",
                    content = @Content(schema = @Schema(implementation = VerificationCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid landlord profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/me/verification-cases")
    public ResponseEntity<VerificationCaseResponse> submitVerificationCase(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitVerificationCaseRequest request
    ) {
        return ResponseEntity.ok(verificationService.submitVerificationCase(jwt.getSubject(), request));
    }

    @Operation(
            summary = "Get Verification Status",
            description = "Retrieves current landlord verification status and history of submitted document cases."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VerificationStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Landlord profile not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me/verification-status")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(verificationService.getVerificationStatus(jwt.getSubject()));
    }

    @Operation(
            summary = "Save Listing (Bookmark)",
            description = "Bookmarks a rental listing to user's favorites list."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing saved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/me/saved-listings/{listingId}")
    public ResponseEntity<Void> saveListing(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId
    ) {
        accountService.saveListing(jwt.getSubject(), listingId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Remove Saved Listing",
            description = "Removes a bookmarked rental listing from user's favorites list."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Listing removed from saved list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/me/saved-listings/{listingId}")
    public ResponseEntity<Void> removeSavedListing(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Listing unique ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID listingId
    ) {
        accountService.removedSavedListing(jwt.getSubject(), listingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get Saved Listings",
            description = "Returns a list of listing UUIDs bookmarked by the user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved listing IDs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me/saved-listings")
    public ResponseEntity<List<UUID>> getSavedListings(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(accountService.getSavedListing(jwt.getSubject()));
    }
}
