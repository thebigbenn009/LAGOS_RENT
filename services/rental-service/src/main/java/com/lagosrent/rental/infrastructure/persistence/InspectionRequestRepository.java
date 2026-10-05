package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.InspectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionRequestRepository extends JpaRepository<InspectionRequest, UUID> {
    List<InspectionRequest> findByTenantKeycloakId(String tenantKeycloakId);

    List<InspectionRequest> findByLandlordKeycloakId(String landlordKeycloakId);

    List<InspectionRequest> findByListingId(UUID listingId);
}
