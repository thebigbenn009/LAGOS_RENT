package com.lagosrent.rental.api.controller;

import com.lagosrent.rental.api.dto.ApplicationResponse;
import com.lagosrent.rental.api.dto.CreateApplicationRequest;
import com.lagosrent.rental.api.dto.DecideApplicationRequest;
import com.lagosrent.rental.application.service.RentalApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/rentals/applications")
@RequiredArgsConstructor
public class RentalApplicationController {
    private final RentalApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> createDraftApplication(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(applicationService.createDraftApplication(jwt.getSubject(), request));
    }

    @PostMapping("/{applicationId}/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(applicationService.submitApplication(applicationId, jwt.getSubject()));
    }

    @PostMapping("/{applicationId}/accept")
    public ResponseEntity<ApplicationResponse> acceptApplication(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(applicationService.acceptApplication(applicationId, jwt.getSubject()));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DecideApplicationRequest request
    ) {
        return ResponseEntity.ok(applicationService.rejectApplication(applicationId, jwt.getSubject(), request));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(applicationService.getApplicationById(applicationId));
    }


    @GetMapping("/my-applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(applicationService.getTenantApplications(jwt.getSubject()));
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<ApplicationResponse>> getListingApplications(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(applicationService.getListingApplications(listingId, jwt.getSubject()));
    }
}
