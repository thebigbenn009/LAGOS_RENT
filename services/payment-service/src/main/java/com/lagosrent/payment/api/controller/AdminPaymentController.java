package com.lagosrent.payment.api.controller;

import com.lagosrent.payment.api.dto.AdminReprocessWebhookRequest;
import com.lagosrent.payment.api.dto.AdminTransactionResponse;
import com.lagosrent.payment.api.dto.AdminWebhookResponse;
import com.lagosrent.payment.application.service.AdminPaymentService;
import com.lagosrent.payment.domain.model.WebhookStatus;
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

import java.util.UUID;

@RestController
@Tag(name = "Admin Payments", description = "Privileged payment reconciliation and webhook management")
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminPaymentController {
    private final AdminPaymentService adminPaymentService;

    @Operation(summary = "Query Webhooks", description = "List raw provider webhooks with optional status filter")
    @GetMapping("/webhooks")
    public ResponseEntity<Page<AdminWebhookResponse>> getWebhooks(
            @RequestParam(required = false) WebhookStatus status,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminPaymentService.getWebhooks(status, pageable));
    }

    @Operation(summary = "Query Transactions", description = "List confirmed payment transactions")
    @GetMapping("/transactions")
    public ResponseEntity<Page<AdminTransactionResponse>> getTransactions(
            @PageableDefault(size = 20, sort = "paidAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminPaymentService.getTransactions(pageable));
    }

    @Operation(summary = "Reprocess Webhook", description = "Manually reprocesses an unhandled or failed webhook payload")
    @PostMapping("/webhooks/{id}/reprocess")
    public ResponseEntity<AdminWebhookResponse> reprocessWebhook(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminReprocessWebhookRequest request
    ) {
        return ResponseEntity.ok(adminPaymentService.reprocessWebhook(id, jwt.getSubject(), request));
    }
}
