package com.lagosrent.payment.infrastructure.persistence;

import com.lagosrent.payment.domain.model.Payable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayableRepository extends JpaRepository<Payable, UUID> {
    Optional<Payable> findByApplicationId(UUID applicationId);

    Optional<Payable> findByReservationReference(String reservationReference);

    List<Payable> findByTenantKeycloakId(String tenantKeycloakId);
}
