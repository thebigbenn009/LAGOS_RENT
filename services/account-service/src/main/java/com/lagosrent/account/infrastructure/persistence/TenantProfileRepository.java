package com.lagosrent.account.infrastructure.persistence;

import com.lagosrent.account.domain.model.TenantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantProfileRepository extends JpaRepository<TenantProfile, UUID> {
    Optional<TenantProfile> findByAccountId(UUID accountId);
}
