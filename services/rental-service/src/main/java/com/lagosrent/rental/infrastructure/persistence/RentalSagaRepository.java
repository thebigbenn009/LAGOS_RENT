package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.RentalSaga;
import com.lagosrent.rental.domain.model.SagaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RentalSagaRepository extends JpaRepository<RentalSaga, UUID> {
    Optional<RentalSaga> findByApplicationId(UUID applicationId);

    Optional<RentalSaga> findByReservationReference(String reservationReference);

    Page<RentalSaga> findAllByStatus(SagaStatus status, Pageable pageable);
}
