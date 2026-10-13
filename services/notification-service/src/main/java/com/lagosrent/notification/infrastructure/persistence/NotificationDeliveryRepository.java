package com.lagosrent.notification.infrastructure.persistence;

import com.lagosrent.notification.domain.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    Optional<NotificationDelivery> findByEventIdAndTemplateCodeAndReceipt(UUID eventId, String templateCode, String recipient);

    List<NotificationDelivery> findByRecipient(String recipient);
}
