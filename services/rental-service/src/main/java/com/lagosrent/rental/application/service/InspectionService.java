package com.lagosrent.rental.application.service;

import com.lagosrent.rental.api.dto.ApproveInspectionRequest;
import com.lagosrent.rental.api.dto.InspectionResponse;
import com.lagosrent.rental.api.dto.RequestInspectionRequest;
import com.lagosrent.rental.application.exception.ResourceNotFoundException;
import com.lagosrent.rental.domain.model.InspectionRequest;
import com.lagosrent.rental.infrastructure.client.ListingClient;
import com.lagosrent.rental.infrastructure.persistence.InspectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.lagosrent.rental.domain.model.InspectionStatus.*;

@Service
@RequiredArgsConstructor
public class InspectionService {
    private final InspectionRequestRepository inspectionRepository;
    private final ListingClient listingClient;

    @Transactional
    public InspectionResponse requestInspection(String tenantKeycloakId, RequestInspectionRequest request) {
        ListingClient.ListingDetailDto listing = listingClient.getListing(request.listingId());
        if (!"PUBLISHED".equals(listing.status())) {
            throw new IllegalStateException("Inspection can only be requested on PUBLISHED listings. Current status: " + listing.status());
        }
        InspectionRequest inspection = new InspectionRequest(
                request.listingId(),
                tenantKeycloakId,
                listing.landlordKeycloakId(),
                request.proposedDatetime(),
                request.notes()
        );

        return InspectionResponse.from(inspectionRepository.save(inspection));
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> getTenantInspections(String tenantKeycloakId) {
        return inspectionRepository.findByTenantKeycloakId(tenantKeycloakId).stream()
                .map(InspectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> getLandlordInspections(String landlordKeycloakId) {
        return inspectionRepository.findByLandlordKeycloakId(landlordKeycloakId).stream()
                .map(InspectionResponse::from)
                .toList();
    }

    @Transactional
    public InspectionResponse approveInspection(UUID inspectionId, String landlordKeycloakId, ApproveInspectionRequest request) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("InspectionRequest", inspectionId));

        if (!inspection.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this listing inspection");
        }

        if (inspection.getStatus() != REQUESTED && inspection.getStatus() != RESCHEDULED) {
            throw new IllegalArgumentException("Cannot approve inspection in status: " + inspection.getStatus());
        }

        inspection.setStatus(APPROVED);
        inspection.setConfirmedDatetime(request.confirmedDatetime());

        return InspectionResponse.from(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionResponse completeInspection(UUID inspectionId, String landlordKeycloakId) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("InspectionRequest", inspectionId));

        if (!inspection.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this listing inspection");
        }

        if (inspection.getStatus() != APPROVED) {
            throw new IllegalStateException("Only APPROVED inspections can be marked COMPLETED");
        }

        inspection.setStatus(COMPLETED);
        return InspectionResponse.from(inspectionRepository.save(inspection));
    }
}
