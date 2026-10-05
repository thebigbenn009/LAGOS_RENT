package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.RentalSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RentalSagaRepository extends JpaRepository<RentalSaga, UUID> {
    Optional<RentalSaga> findByApplicationId(UUID applicationId);

    Optional<RentalSaga> findByReservationReference(String reservationReference);
}
