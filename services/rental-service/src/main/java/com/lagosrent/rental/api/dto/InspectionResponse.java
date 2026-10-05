package com.lagosrent.rental.api.dto;

import com.lagosrent.rental.domain.model.InspectionRequest;
import com.lagosrent.rental.domain.model.InspectionStatus;

import java.time.Instant;
import java.util.UUID;

public record InspectionResponse(
        UUID id,
        UUID listingId,
        String tenantKeycloakId,
        String landlordKeycloakId,
        Instant proposedDatetime,
        Instant confirmedDatetime,
        InspectionStatus status,
        String notes,
        Instant createdAt
) {
    public static InspectionResponse from(InspectionRequest req) {
        return new InspectionResponse(
                req.getId(),
                req.getListingId(),
                req.getTenantKeycloakId(),
                req.getLandlordKeycloakId(),
                req.getProposedDatetime(),
                req.getConfirmedDatetime(),
                req.getStatus(),
                req.getNotes(),
                req.getCreatedAt()
        );
    }
}
