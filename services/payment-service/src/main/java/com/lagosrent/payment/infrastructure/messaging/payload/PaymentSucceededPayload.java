package com.lagosrent.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededPayload(
        UUID paymentId,
        UUID applicationId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        BigDecimal amount,
        String currency,
        String providerReference,
        String receiptNumber,
        Instant paidAt
) {}
