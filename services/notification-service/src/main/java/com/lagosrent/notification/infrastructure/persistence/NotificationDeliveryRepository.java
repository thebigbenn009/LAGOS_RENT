package com.lagosrent.notification.infrastructure.persistence;

import com.lagosrent.notification.domain.model.DeliveryStatus;
import com.lagosrent.notification.domain.model.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    Optional<NotificationDelivery> findByEventIdAndTemplateCodeAndRecipient(UUID eventId, String templateCode, String recipient);

    List<NotificationDelivery> findByRecipient(String recipient);

    Page<NotificationDelivery> findAllByStatus(DeliveryStatus status, Pageable pageable);
}
