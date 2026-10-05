package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByReservationReference(String reservationReference);

    Optional<Reservation> findByApplicationId(UUID applicationId);
}
