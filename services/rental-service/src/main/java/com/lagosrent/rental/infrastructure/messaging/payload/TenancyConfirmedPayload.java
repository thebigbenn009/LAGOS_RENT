package com.lagosrent.rental.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TenancyConfirmedPayload(
        UUID tenancyId,
        UUID applicationId,
        String listingId,
        String tenantKeycloakId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal annualRent,
        Instant confirmedAt
) {}
