package com.lagosrent.payment.api.controller;

import com.lagosrent.payment.api.dto.CreatePayableRequest;
import com.lagosrent.payment.api.dto.InitializePaymentResponse;
import com.lagosrent.payment.api.dto.PayableResponse;
import com.lagosrent.payment.application.service.PayableService;
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
@RequestMapping("/api/v1/payments/payables")
@RequiredArgsConstructor
public class PayableController {
    private final PayableService payableService;

    @PostMapping
    public ResponseEntity<PayableResponse> createPayable(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePayableRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(payableService.createPayable(jwt.getSubject(), request));
    }

    @PostMapping("/{payableId}/initialize")
    public ResponseEntity<InitializePaymentResponse> initializeCheckout(
            @PathVariable UUID payableId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String tenantEmail = jwt.getClaimAsString("email");
        if (tenantEmail == null || tenantEmail.isBlank()) {
            tenantEmail = jwt.getClaimAsString("preferred_username") + "@lagosrent.internal";
        }
        return ResponseEntity.ok(payableService.initializeCheckout(
                payableId,
                jwt.getSubject(),
                tenantEmail,
                idempotencyKey
        ));
    }

    @GetMapping("/{payableId}")
    public ResponseEntity<PayableResponse> getPayableById(@PathVariable UUID payableId) {
        return ResponseEntity.ok(payableService.getPayableById(payableId));
    }

    @GetMapping("/my-payables")
    public ResponseEntity<List<PayableResponse>> getMyPayables(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(payableService.getTenantPayables(jwt.getSubject()));
    }
}
