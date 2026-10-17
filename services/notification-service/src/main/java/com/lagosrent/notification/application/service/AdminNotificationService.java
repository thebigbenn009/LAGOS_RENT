package com.lagosrent.notification.application.service;

import com.lagosrent.notification.api.dto.AdminDeliveryResponse;
import com.lagosrent.notification.api.dto.AdminRedriveRequest;
import com.lagosrent.notification.api.dto.AdminTemplateResponse;
import com.lagosrent.notification.application.exception.ResourceNotFoundException;
import com.lagosrent.notification.domain.model.DeliveryStatus;
import com.lagosrent.notification.domain.model.NotificationDelivery;
import com.lagosrent.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.lagosrent.notification.infrastructure.persistence.NotificationTemplateRepository;
import com.lagosrent.notification.infrastructure.provider.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.lagosrent.notification.domain.model.DeliveryStatus.FAILED;
import static com.lagosrent.notification.domain.model.DeliveryStatus.SENT;
import static com.lagosrent.notification.infrastructure.provider.EmailProvider.EmailDispatchCommand;
import static com.lagosrent.notification.infrastructure.provider.EmailProvider.EmailDispatchResult;
import static java.time.Instant.now;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailProvider emailProvider;

    @Transactional(readOnly = true)
    public Page<AdminDeliveryResponse> getDeliveries(DeliveryStatus status, Pageable pageable) {
        Page<NotificationDelivery> deliveries = (status != null)
                ? deliveryRepository.findAllByStatus(status, pageable)
                : deliveryRepository.findAll(pageable);

        return deliveries.map(AdminDeliveryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminDeliveryResponse> getFailures(Pageable pageable) {
        return deliveryRepository.findAllByStatus(FAILED, pageable)
                .map(AdminDeliveryResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AdminTemplateResponse> getTemplate() {
        return templateRepository.findAll().stream()
                .map(AdminTemplateResponse::from)
                .toList();
    }

    @Transactional
    public AdminDeliveryResponse redriveDelivery(UUID deliveryId, String adminKeycloakId, AdminRedriveRequest request) {
        NotificationDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationDelivery", deliveryId));

        log.info("Admin {} initiating manual redrive for delivery {}. Reason: {}", adminKeycloakId, deliveryId, request.reason());

        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        EmailDispatchCommand command = new EmailDispatchCommand(
                delivery.getRecipient(),
                delivery.getSubject(),
                delivery.getBody(),
                true
        );

        EmailDispatchResult result = emailProvider.sendEmail(command);

        if (result.success()) {
            delivery.setStatus(SENT);
            delivery.setProviderName(emailProvider.getProviderName());
            delivery.setProviderMessageId(result.providerMessageId());
            delivery.setSentAt(now());
            delivery.setErrorMessage(null);
            log.info("Manual redrive succeeded for delivery {}", deliveryId);
        } else {
            delivery.setStatus(FAILED);
            delivery.setErrorMessage(result.errorMessage());
            log.warn("Manual redrive failed for delivery {}: {}", deliveryId, result.errorMessage());
        }
        return AdminDeliveryResponse.from(deliveryRepository.save(delivery));
    }
}
