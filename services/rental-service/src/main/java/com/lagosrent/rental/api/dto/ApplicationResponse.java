package com.lagosrent.rental.api.dto;

import com.lagosrent.rental.domain.model.ApplicationStatus;
import com.lagosrent.rental.domain.model.RentalApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID listingId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        ApplicationStatus status,
        String employment,
        BigDecimal annualIncome,
        String previousLandlordContact,
        PriceSnapshotResponse priceSnapshot,
        Instant createdAt
) {
    public static ApplicationResponse from(RentalApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getListingId(),
                app.getTenantKeycloakId(),
                app.getLandlordKeycloakId(),
                app.getStatus(),
                app.getEmploymentStatus(),
                app.getAnnualIncome(),
                app.getPreviousLandlordContact(),
                PriceSnapshotResponse.from(app.getPriceSnapshot()),
                app.getCreatedAt()
        );
    }
}
