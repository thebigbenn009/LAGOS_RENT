package com.lagosrent.payment.api.dto;

import com.lagosrent.payment.domain.model.Payable;
import com.lagosrent.payment.domain.model.PayableStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayableResponse(
        UUID id,
        UUID applicationId,
        UUID listingId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        String reservationReference,
        BigDecimal totalAmount,
        String currency,
        PayableStatus status,
        Instant dueDate,
        Instant createdAt
) {
    public static PayableResponse from(Payable p) {
        return new PayableResponse(
                p.getId(),
                p.getApplicationId(),
                p.getListingId(),
                p.getTenantKeycloakId(),
                p.getLandlordKeycloakId(),
                p.getReservationReference(),
                p.getTotalAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getDueDate(),
                p.getCreatedAt()
        );
    }
}
