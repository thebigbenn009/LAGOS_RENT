package com.lagosrent.payment.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InitializePaymentResponse(
        UUID attemptId,
        UUID payableId,
        String internalReference,
        String providerReference,
        String authorizationUrl,
        BigDecimal amount,
        String currency
) {
}
