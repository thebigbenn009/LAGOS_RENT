package com.lagosrent.rental.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record RentalReservationRequestedPayload(
        UUID applicationId,
        UUID listingId,
        String tenantKeycloakId,
        String reservationReference,
        Instant holdDeadline,
        Instant requestedAt
) {}
