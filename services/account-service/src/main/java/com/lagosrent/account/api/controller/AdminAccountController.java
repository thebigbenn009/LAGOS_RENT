package com.lagosrent.account.api.controller;

import com.lagosrent.account.api.dto.AccountResponse;
import com.lagosrent.account.api.dto.AdminReviewVerificationRequest;
import com.lagosrent.account.api.dto.AdminSuspendAccountRequest;
import com.lagosrent.account.api.dto.VerificationCaseResponse;
import com.lagosrent.account.application.service.AdminAccountService;
import com.lagosrent.account.application.service.VerificationService;
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

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole(SUPER_ADMIN)")
public class AdminAccountController {
    private final AdminAccountService adminAccountService;
    private final VerificationService verificationService;

    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountResponse>> listAccounts(Pageable pageable) {
        return ResponseEntity.ok(adminAccountService.listAccounts(pageable));
    }

    @PostMapping("/accounts/{id}/suspend")
    public ResponseEntity<AccountResponse> suspendAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminSuspendAccountRequest request
    ) {
        return ResponseEntity.ok(adminAccountService.suspendAccount(id, jwt.getSubject(), request.reason()));
    }

    @PostMapping("/accounts/{id}/reactivate")
    public ResponseEntity<AccountResponse> reactivateAccount(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminSuspendAccountRequest request
    ) {
        return ResponseEntity.ok(adminAccountService.reactivateAccount(id, jwt.getSubject(), request.reason()));
    }

    @PostMapping("/verification-cases/{id}/approve")
    public ResponseEntity<VerificationCaseResponse> approveVerificationCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewVerificationRequest request
    ) {
        return ResponseEntity.ok(verificationService.approveVerificationCase(id, jwt.getSubject(), request.reason()));
    }

    @PostMapping("/verification-cases/{id}/reject")
    public ResponseEntity<VerificationCaseResponse> rejectVerificationCase(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReviewVerificationRequest request
    ) {
        return ResponseEntity.ok(verificationService.rejectVerificationCase(id, jwt.getSubject(), request.reason()));
    }
}
