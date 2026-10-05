package com.lagosrent.rental.api.controller;

import com.lagosrent.rental.api.dto.ApproveInspectionRequest;
import com.lagosrent.rental.api.dto.InspectionResponse;
import com.lagosrent.rental.api.dto.RequestInspectionRequest;
import com.lagosrent.rental.application.service.InspectionService;
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
@RequestMapping("/api/v1/rentals/inspections")
@RequiredArgsConstructor
public class InspectionController {
    private final InspectionService inspectionService;

    @PostMapping
    public ResponseEntity<InspectionResponse> requestInspection(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RequestInspectionRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(inspectionService.requestInspection(jwt.getSubject(), request));
    }

    @GetMapping("/my-inspections")
    public ResponseEntity<List<InspectionResponse>> getMyInspections(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(inspectionService.getTenantInspections(jwt.getSubject()));
    }

    @GetMapping("/landlord")
    public ResponseEntity<List<InspectionResponse>> getLandlordInspections(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(inspectionService.getLandlordInspections(jwt.getSubject()));
    }

    @PostMapping("/{inspectionId}/approve")
    public ResponseEntity<InspectionResponse> approveInspection(
            @PathVariable UUID inspectionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApproveInspectionRequest request
    ) {
        return ResponseEntity.ok(inspectionService.approveInspection(inspectionId, jwt.getSubject(), request));
    }

    @PostMapping("/{inspectionId}/complete")
    public ResponseEntity<InspectionResponse> completeInspection(
            @PathVariable UUID inspectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(inspectionService.completeInspection(inspectionId, jwt.getSubject()));
    }
}
