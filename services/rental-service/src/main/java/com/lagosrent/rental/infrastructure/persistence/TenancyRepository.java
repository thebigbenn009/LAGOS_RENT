package com.lagosrent.rental.infrastructure.persistence;

import com.lagosrent.rental.domain.model.Tenancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenancyRepository extends JpaRepository<Tenancy, UUID> {
    Optional<Tenancy> findByApplicationId(UUID applicationId);
}
