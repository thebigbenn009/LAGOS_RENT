package com.lagosrent.rental.api.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.lagosrent.rental.domain.model.PriceSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceSnapshotResponse(
        UUID id,
        UUID listingId,
        Long listingVersion,
        BigDecimal rentAmount,
        BigDecimal cautionDeposit,
        BigDecimal serviceCharge,
        BigDecimal legalFee,
        BigDecimal totalMoveInAmount,
        String currency,
        @JsonRawValue
        String feeBreakdown,
        Instant capturedAt
) {
    public static PriceSnapshotResponse from(PriceSnapshot snap) {
        if (snap == null) return null;
        return new PriceSnapshotResponse(
                snap.getId(),
                snap.getListingId(),
                snap.getListingVersion(),
                snap.getRentAmount(),
                snap.getCautionDeposit(),
                snap.getServiceCharge(),
                snap.getLegalFee(),
                snap.getTotalMoveInAmount(),
                snap.getCurrency(),
                snap.getFeeBreakdown(),
                snap.getCapturedAt()
        );
    }
}
