package com.lagosrent.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePayableRequest(
        @NotNull UUID applicationId,
        @NotNull UUID listingId,
        @NotBlank String landlordKeycloakId,
        @NotBlank String reservationReference,
        @NotNull @DecimalMin("1.00")
        BigDecimal totalAmount,
        String currency,
        Instant dueDate
) {
}
