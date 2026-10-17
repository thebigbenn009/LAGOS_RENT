package com.lagosrent.rental.api.controller;

import com.lagosrent.rental.api.dto.AdminSagaResponse;
import com.lagosrent.rental.application.service.AdminRentalService;
import com.lagosrent.rental.domain.model.SagaStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin Rentals", description = "Privileged rental saga monitoring and inspection")
@RestController
@RequestMapping("/api/v1/admin/rentals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminRentalController {
    private final AdminRentalService adminRentalService;

    @Operation(summary = "List Rental Sagas", description = "Query distributed sagas with optional status filter")
    @GetMapping("/sagas")
    public ResponseEntity<Page<AdminSagaResponse>> getSagas(
            @RequestParam(required = false) SagaStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminRentalService.getSagas(status, pageable));
    }

    @Operation(summary = "Get Saga Details", description = "Inspect a specific rental saga by its ID")
    @GetMapping("/sagas/{id}")
    public ResponseEntity<AdminSagaResponse> getSagaById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminRentalService.getSagaById(id));
    }


}
