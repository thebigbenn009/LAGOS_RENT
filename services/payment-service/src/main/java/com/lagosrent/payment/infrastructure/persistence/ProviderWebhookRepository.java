package com.lagosrent.payment.infrastructure.persistence;

import com.lagosrent.payment.domain.model.ProviderWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderWebhookRepository extends JpaRepository<ProviderWebhook, UUID> {
    Optional<ProviderWebhook> findByProviderNameAndEventTypeAndProviderReference(String providerName, String eventType, String providerReference);
}
