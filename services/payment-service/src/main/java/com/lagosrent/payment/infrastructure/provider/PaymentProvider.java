package com.lagosrent.payment.infrastructure.provider;

import java.math.BigDecimal;

public interface PaymentProvider {
    String getProviderName();

    PaymentInitResult initializePayment(PaymentInitCommand command);

    PaymentVerificationResult verifyPayment(String providerReference);

    boolean validateWebhookSignature(String rawPayload, String signatureHeader);

    record PaymentInitCommand(
            String internalReference,
            BigDecimal amount,
            String currency,
            String customerEmail,
            String callbackUrl
    ) {}

    record PaymentInitResult(
            boolean success,
            String providerReference,
            String authorizationUrl,
            String errorMessage
    ) {}

    record PaymentVerificationResult(
            boolean success,
            String status /* "success", "failed", "abandoned" */,
            String providerReference,
            String internalReference,
            BigDecimal amount,
            String currency,
            String channel,
            String errorMessage
    ) {}

}
