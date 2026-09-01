package com.lagosrent.account.infrastructure.persistence;

import com.lagosrent.account.domain.model.AdminAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminAuditRecordRepository extends JpaRepository<AdminAuditRecord, UUID> {
    List<AdminAuditRecord> findByEntityTypeAndEntityId(String entityType, String entityId);
}
