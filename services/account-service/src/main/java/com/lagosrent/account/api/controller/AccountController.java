package com.lagosrent.account.api.controller;

import com.lagosrent.account.api.dto.*;
import com.lagosrent.account.application.service.AccountService;
import com.lagosrent.account.application.service.VerificationService;
import com.lagosrent.account.domain.model.Account;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final VerificationService verificationService;

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getCurrentAccount(@AuthenticationPrincipal Jwt jwt) {
        Account account = accountService.getOrCreateAccountFromJwt(jwt);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PutMapping("/me/tenant-profile")
    public ResponseEntity<TenantProfileResponse> updateTenantProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateTenantProfileRequest request
    ) {
        return ResponseEntity.ok(accountService.updateTenantProfile(jwt.getSubject(), request));
    }

    @GetMapping("/me/tenant-profile")
    public ResponseEntity<TenantProfileResponse> getTenantProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getTenantProfile(jwt.getSubject()));
    }

    @PutMapping("/me/landlord-profile")
    public ResponseEntity<LandlordProfileResponse>updateLandlordProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid, @RequestBody UpdateLandlordProfileRequest request
            ){
        return ResponseEntity.ok(accountService.updateLandlordProfile(jwt.getSubject(), request));
    }

    @GetMapping("/me/landlord-profile")
    public ResponseEntity<LandlordProfileResponse>getLandlordProfile(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getLandlordProfile(jwt.getSubject()));
    }

    @PostMapping("/me/verification-cases")
    public ResponseEntity<VerificationCaseResponse>submitVerificationCase(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitVerificationCaseRequest request
    ){
        return ResponseEntity.ok(verificationService.submitVerificationCase(jwt.getSubject(), request));
    }

    @GetMapping("/me/verification-status")
    public ResponseEntity<VerificationStatusResponse>getVerificationStatus(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(verificationService.getVerificationStatus(jwt.getSubject()));
    }

    @PostMapping("/me/saved-listings/{listingId}")
    public ResponseEntity<Void>saveListing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listingId
            ){
        accountService.saveListing(jwt.getSubject(), listingId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/saved-listings/{listingId}")
    public ResponseEntity<Void>removeSavedListing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listingId
    ){
        accountService.removedSavedListing(jwt.getSubject(), listingId);
        return  ResponseEntity.noContent().build();
    }

    @GetMapping("/me/saved-listings")
    public ResponseEntity<List<UUID>>getSavedListings(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(accountService.getSavedListing(jwt.getSubject()));
    }
}
