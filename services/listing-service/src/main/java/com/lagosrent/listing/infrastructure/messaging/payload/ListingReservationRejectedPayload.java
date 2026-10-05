package com.lagosrent.listing.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record ListingReservationRejectedPayload(
        UUID listingId,
        String reservationReference,
        String reason,
        Instant rejectedAt
) {}
