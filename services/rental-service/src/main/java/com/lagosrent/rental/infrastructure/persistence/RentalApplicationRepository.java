package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.ApplicationStatus;
import com.lagosrent.rental.domain.model.RentalApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RentalApplicationRepository extends JpaRepository<RentalApplication, UUID> {
    List<RentalApplication> findByTenantKeycloakId(String tenantKeycloakId);

    List<RentalApplication> findByListingId(UUID listingId);

    Optional<RentalApplication> findByListingIdAndTenantKeycloakIdAndStatusNotIn(
            UUID listingId,
            String tenantKeycloakId,
            List<ApplicationStatus> terminalStatuses
    );
}
