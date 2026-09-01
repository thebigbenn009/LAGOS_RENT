package com.lagosrent.account.infrastructure.persistence;

import com.lagosrent.account.domain.model.VerificationCase;
import com.lagosrent.account.domain.model.VerificationCaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VerificationCaseRepository extends JpaRepository<VerificationCase, UUID> {
    List<VerificationCase> findByLandlordProfileId(UUID landlordProfileId);

    List<VerificationCase> findByStatus(VerificationCaseStatus status);
}
