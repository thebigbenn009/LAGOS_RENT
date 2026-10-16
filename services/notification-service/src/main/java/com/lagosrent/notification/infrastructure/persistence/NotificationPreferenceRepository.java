package com.lagosrent.notification.infrastructure.persistence;

import com.lagosrent.notification.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByUserKeycloakId(String userKeycloakId);
}
