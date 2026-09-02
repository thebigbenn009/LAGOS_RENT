package com.lagosrent.account.infrastructure.persistence;

import com.lagosrent.account.domain.model.LandlordProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LandlordProfileRepository extends JpaRepository<LandlordProfile, UUID> {
    Optional<LandlordProfile> findByAccountId(UUID accountId);
}
