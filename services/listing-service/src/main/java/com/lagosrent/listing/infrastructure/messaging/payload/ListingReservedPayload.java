package com.lagosrent.listing.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record ListingReservedPayload(
        UUID listingId,
        String reservationReference,
        Instant heldUntil,
        Instant reservedAt
) {}
