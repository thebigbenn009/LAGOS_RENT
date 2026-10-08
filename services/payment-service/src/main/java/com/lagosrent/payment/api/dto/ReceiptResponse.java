package com.lagosrent.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.lagosrent.payment.domain.model.Receipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID transactionId,
        UUID payableId,
        String receiptNumber,
        BigDecimal amount,
        String currency,
        String issuedToTenantKeycloakId,
        String issuedToLandlordKeycloakId,
        @JsonRawValue String receiptData,
        Instant issuedAt
) {
    public static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(
                r.getId(),
                r.getTransaction().getId(),
                r.getPayable().getId(),
                r.getReceiptNumber(),
                r.getAmount(),
                r.getCurrency(),
                r.getIssuedToTenantKeycloakId(),
                r.getIssuedForLandlordKeycloakId(),
                r.getReceiptData(),
                r.getIssuedAt()
        );
    }
}
