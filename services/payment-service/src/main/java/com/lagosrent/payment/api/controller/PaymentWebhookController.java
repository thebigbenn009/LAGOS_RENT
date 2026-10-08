package com.lagosrent.payment.api.controller;

import com.lagosrent.payment.application.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    private final PaymentWebhookService webhookService;

    @PostMapping("/paystack")
    public ResponseEntity<String> handlePaystackWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signatureHeader
    ) {
        log.info("Received Paystack webhook notification");
        webhookService.processPaystackWebhook(rawPayload, signatureHeader);
        return ResponseEntity.ok("Webhook received and processed");
    }
}
