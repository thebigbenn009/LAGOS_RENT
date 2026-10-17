package com.lagosrent.rental.api.dto;

import com.lagosrent.rental.domain.model.RentalSaga;
import com.lagosrent.rental.domain.model.SagaStatus;
import com.lagosrent.rental.domain.model.SagaStep;

import java.time.Instant;
import java.util.UUID;

public record AdminSagaResponse(
        UUID id,
        UUID applicationId,
        UUID listingId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        String reservationReference,
        SagaStep currentStep,
        SagaStatus status,
        Instant createdAt,
        Instant UpdatedAt
) {
    public static AdminSagaResponse from(RentalSaga saga) {
        return new AdminSagaResponse(
                saga.getId(),
                saga.getApplication().getId(),
                saga.getListingId(),
                saga.getApplication().getTenantKeycloakId(),
                saga.getApplication().getLandlordKeycloakId(),
                saga.getReservationReference(),
                saga.getCurrentStep(),
                saga.getStatus(),
                saga.getCreatedAt(),
                saga.getUpdatedAt()
        );
    }
}
