package com.lagosrent.notification.application.service;

import com.lagosrent.notification.domain.model.NotificationDelivery;
import com.lagosrent.notification.domain.model.NotificationTemplate;
import com.lagosrent.notification.infrastructure.persistence.NotificationDeliveryRepository;
import com.lagosrent.notification.infrastructure.persistence.NotificationPreferenceRepository;
import com.lagosrent.notification.infrastructure.persistence.NotificationTemplateRepository;
import com.lagosrent.notification.infrastructure.provider.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.lagosrent.notification.domain.model.DeliveryStatus.FAILED;
import static com.lagosrent.notification.domain.model.DeliveryStatus.SENT;
import static com.lagosrent.notification.domain.model.NotificationChannel.EMAIL;
import static com.lagosrent.notification.infrastructure.provider.EmailProvider.EmailDispatchCommand;
import static com.lagosrent.notification.infrastructure.provider.EmailProvider.EmailDispatchResult;
import static java.time.Instant.now;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {
    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailProvider emailProvider;
    private final TemplateRenderer templateRenderer;

    /**
     * Strictly idempotent multi-channel notification dispatch.
     * Enforces (eventId, templateCode, recipient) unique constraint.
     */
    @Transactional
    public void dispatch(
            UUID eventId,
            String templateCode,
            String recipientEmail,
            String recipientKeycloakId,
            Map<String, Object> model
    ) {

        // 1. Idempotency Check: Have this notification been delivered to this event?
        Optional<NotificationDelivery> existingDelivery =
                deliveryRepository.findByEventIdAndTemplateCodeAndRecipient(eventId, templateCode, recipientEmail);

        if (existingDelivery.isPresent() && existingDelivery.get().getStatus() == SENT) {
            log.info("Duplicate notification detected and safely ignored: event={}, template={}, recipient={}",
                    eventId, templateCode, recipientEmail);
            return;
        }

        // 2. Preference check (if user has opt-outs)
        if (recipientKeycloakId != null) {
            var preference = preferenceRepository.findByUserKeycloakId(recipientKeycloakId);
            if (preference.isPresent() && !preference.get().isEmailEnabled()) {
                log.info("User disabled email notifications: user={}. Skipping.", recipientKeycloakId);
                return;
            }
        }

        // 3. Load Template
        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown notification template: " + templateCode));

        // 4. Render Template Copy
        String subject = templateRenderer.render(template.getSubjectTemplate(), model);
        String body = templateRenderer.render(template.getBodyTemplate(), model);

        // 5. Persist Delivery Audit Record
        NotificationDelivery delivery = existingDelivery.orElseGet(() ->
                new NotificationDelivery(eventId, templateCode, recipientEmail, EMAIL, subject, body)
        );

        delivery.setSubject(subject);
        delivery.setBody(body);
        delivery.setProviderName(emailProvider.getProviderName());

        // 6. Invoke Active Provider Adapter
        EmailDispatchCommand command = new EmailDispatchCommand(recipientEmail, subject, body, template.isHtml());

        EmailDispatchResult result = emailProvider.sendEmail(command);

        if (result.success()) {
            delivery.setStatus(SENT);
            delivery.setProviderMessageId(result.providerMessageId());
            delivery.setSentAt(now());
            delivery.setErrorMessage(null);
            log.info("Notification successfully sent! eventId={}, template={}, recipient={}",
                    eventId, templateCode, recipientEmail);
        } else {
            delivery.setStatus(FAILED);
            delivery.setErrorMessage(result.errorMessage());
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            log.error("Failed to dispatch notification: {}", result.errorMessage());
        }
        deliveryRepository.save(delivery);
    }
}
