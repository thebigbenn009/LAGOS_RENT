package com.lagosrent.payment.infrastructure.persistence;

import com.lagosrent.payment.domain.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findByInternalReference(String internalReference);

    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);
}
