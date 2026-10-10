package com.lagosrent.rental.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record ListingReleaseRequestedPayload(
        UUID listingId,
        String reservationReference,
        String reason, /* "PAYMENT_FAILED", "PAYMENT_EXPIRED", "APPLICATION_CANCELLED" */
        Instant requestedAt
) {
}
