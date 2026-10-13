package com.lagosrent.rental.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TenancyConfirmedPayload(
        UUID tenancyId,
        UUID applicationId,
        UUID listingId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal annualRent,
        Instant confirmedAt
) {}
