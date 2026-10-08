package com.lagosrent.payment.infrastructure.persistence;

import com.lagosrent.payment.domain.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findByPayableId(UUID payableId);

    List<Receipt> findByIssuedToTenantKeycloakId(String tenantKeycloakId);
}
