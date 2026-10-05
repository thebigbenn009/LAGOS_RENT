package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, UUID> {
    Optional<PriceSnapshot> findByApplicationId(UUID applicationId);
}
