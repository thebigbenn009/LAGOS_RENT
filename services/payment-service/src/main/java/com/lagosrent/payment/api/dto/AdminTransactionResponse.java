package com.lagosrent.payment.api.dto;

import com.lagosrent.payment.domain.model.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminTransactionResponse(
        UUID id,
        UUID payableId,
        String providerReference,
        BigDecimal amount,
        String currency,
        String channel,
        Instant paidAt
) {
    public static AdminTransactionResponse from(PaymentTransaction tx) {
        return new AdminTransactionResponse(
                tx.getId(),
                tx.getPayable().getId(),
                tx.getProviderReference(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getChannel(),
                tx.getPaidAt()
        );
    }
}
