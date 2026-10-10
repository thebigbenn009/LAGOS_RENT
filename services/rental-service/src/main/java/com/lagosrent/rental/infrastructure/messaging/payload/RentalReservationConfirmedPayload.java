package com.lagosrent.rental.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RentalReservationConfirmedPayload(
        UUID applicationId,
        UUID listingId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        String reservationReference,
        BigDecimal totalMoveInAmount,
        String currency,
        Instant holdDeadline,
        Instant confirmedAt
) {
}
