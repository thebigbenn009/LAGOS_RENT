package com.lagosrent.listing.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record LandlordVerifiedPayload(
        String landlordKeycloakId,
        UUID landlordProfileId,
        String verificationType,
        String verifiedByAdminKeycloakId,
        Instant verifiedAt
) {}
