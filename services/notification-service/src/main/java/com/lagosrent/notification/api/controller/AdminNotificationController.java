package com.lagosrent.notification.api.controller;

import com.lagosrent.notification.api.dto.AdminDeliveryResponse;
import com.lagosrent.notification.api.dto.AdminRedriveRequest;
import com.lagosrent.notification.api.dto.AdminTemplateResponse;
import com.lagosrent.notification.application.service.AdminNotificationService;
import com.lagosrent.notification.domain.model.DeliveryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin Notifications", description = "Privileged notification delivery audit ledger and manual redrive operations")
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminNotificationController {
    private final AdminNotificationService adminNotificationService;

    @Operation(summary = "List Deliveries", description = "Query all notification delivery records with optional status filter")
    @GetMapping("/deliveries")
    public ResponseEntity<Page<AdminDeliveryResponse>> getDeliveries(
            @RequestParam(required = false) DeliveryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminNotificationService.getDeliveries(status, pageable));
    }

    @Operation(summary = "List Delivery Failures", description = "Shortcut to retrieve only failed notifications requiring administrative attention")
    @GetMapping("/failures")
    public ResponseEntity<Page<AdminDeliveryResponse>> getFailures(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminNotificationService.getFailures(pageable));
    }

    @Operation(summary = "List Templates", description = "List all active notification templates and version numbers")
    @GetMapping("/templates")
    public ResponseEntity<List<AdminTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(adminNotificationService.getTemplate());
    }

    @Operation(summary = "Manual Delivery Redrive", description = "Re-dispatches a failed notification attempt with mandatory audit reason")
    @PostMapping("/deliveries/{id}/redrive")
    public ResponseEntity<AdminDeliveryResponse> redriveDelivery(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminRedriveRequest request
    ) {
        return ResponseEntity.ok(adminNotificationService.redriveDelivery(id, jwt.getSubject(), request));
    }
}
