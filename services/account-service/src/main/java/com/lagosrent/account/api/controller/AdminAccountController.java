package com.lagosrent.account.api.controller;

import com.lagosrent.account.api.dto.AccountResponse;
import com.lagosrent.account.api.dto.AdminReviewVerificationRequest;
import com.lagosrent.account.api.dto.AdminSuspendAccountRequest;
import com.lagosrent.account.api.dto.VerificationCaseResponse;
import com.lagosrent.account.api.error.ErrorResponse;
import com.lagosrent.account.application.service.AdminAccountService;
import com.lagosrent.account.application.service.VerificationService;
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

@Tag(name = "Admin Accounts", description = "Administrative account moderation and KYC document verification review")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminAccountController {
    private final AdminAccountService adminAccountService;
    private final VerificationService verificationService;

    @Operation(
            summary = "List User Accounts",
            description = "Retrieves a paginated list of all registered accounts in the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required")
    })
    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountResponse>> listAccounts(Pageable pageable) {
        return ResponseEntity.ok(adminAccountService.listAccounts(pageable));
    }

    @Operation(
            summary = "Suspend Account",
            description = "Suspends a user account with mandatory administrative audit justification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account suspended successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/accounts/{id}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(
            @Parameter(description = "Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminSuspendAccountRequest request
    ) {
        return ResponseEntity.ok(adminAccountService.suspendAccount(id, jwt.getSubject(), request.reason()));
    }

    @Operation(
            summary = "Reactivate Account",
            description = "Reactivates a previously suspended user account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account reactivated successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/accounts/{id}/reactivate")
    public ResponseEntity<AccountResponse> reactivateAccount(
            @Parameter(description = "Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminSuspendAccountRequest request
    ) {
        return ResponseEntity.ok(adminAccountService.reactivateAccount(id, jwt.getSubject(), request.reason()));
    }

    @Operation(
            summary = "Approve KYC Verification Document",
            description = "Approves a landlord document verification case and updates landlord verification status if criteria are satisfied."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification case approved successfully",
                    content = @Content(schema = @Schema(implementation = VerificationCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid case status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Verification case not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/verification-cases/{id}/approve")
    public ResponseEntity<VerificationCaseResponse> approveVerificationCase(
            @Parameter(description = "Verification case ID", example = "550e8400-e29b-41d4-a716-446655440003")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewVerificationRequest request
    ) {
        return ResponseEntity.ok(verificationService.approveVerificationCase(id, jwt.getSubject(), request.reason()));
    }

    @Operation(
            summary = "Reject KYC Verification Document",
            description = "Rejects a landlord document verification case with mandatory rejection rationale."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification case rejected successfully",
                    content = @Content(schema = @Schema(implementation = VerificationCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid case status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Verification case not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/verification-cases/{id}/reject")
    public ResponseEntity<VerificationCaseResponse> rejectVerificationCase(
            @Parameter(description = "Verification case ID", example = "550e8400-e29b-41d4-a716-446655440003")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewVerificationRequest request
    ) {
        return ResponseEntity.ok(verificationService.rejectVerificationCase(id, jwt.getSubject(), request.reason()));
    }
}
