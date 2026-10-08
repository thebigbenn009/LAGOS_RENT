package com.lagosrent.payment.api.controller;

import com.lagosrent.payment.api.dto.ReceiptResponse;
import com.lagosrent.payment.application.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/receipts")
@RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptService receiptService;

    @GetMapping("/payable/{payableId}")
    public ResponseEntity<ReceiptResponse> getReceiptByPayableId(@PathVariable UUID payableId) {
        return ResponseEntity.ok(receiptService.getReceiptByPayableId(payableId));
    }

    @GetMapping("/my-receipts")
    public ResponseEntity<List<ReceiptResponse>> getMyReceipts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(receiptService.getTenantReceipts(jwt.getSubject()));
    }
}
