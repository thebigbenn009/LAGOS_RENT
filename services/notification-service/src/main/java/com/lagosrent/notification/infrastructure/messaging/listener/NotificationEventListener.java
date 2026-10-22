package com.lagosrent.notification.infrastructure.messaging.listener;

import com.lagosrent.notification.application.service.NotificationDispatchService;
import com.lagosrent.notification.infrastructure.client.AccountClient;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationDispatchService dispatchService;
    private final AccountClient accountClient;
    private final ObjectMapper objectMapper;
    private final RetryTemplate consumerRetryTemplate;

    @RabbitListener(queues = "notification.events.v1", containerFactory = "rabbitListenerContainerFactory")
    public void onDomainEvent(Message rawMessage, Channel channel) throws Exception {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        String body = new String(rawMessage.getBody());

        try {
            // Execute message processing within bounded retry template (3 attempts with backoff)
            consumerRetryTemplate.execute(context -> {
                processEvent(body);
                return null;
            });

            // Explicitly manual ACK only after successful completion
            channel.basicAck(deliveryTag, false);
            log.info("Message deliveryTag={} acknowledged successfully.", deliveryTag);
        }
        catch (Exception ex) {
            // Reject with requeue = false -> RabbitMQ automatically routes to notification.events.dlq
            log.error("Event processing failed permanently. Dead-lettering deliveryTag={}. Root cause: [{}] {}",
                    deliveryTag, ex.getClass().getName(), ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void processEvent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String eventIdStr = root.path("eventId").asString();
        UUID eventId = eventIdStr.isBlank() ? randomUUID() : fromString(eventIdStr);
        String eventType = root.path("eventType").asString();
        JsonNode payload = root.path("payload");

        log.info("Notification Service received event: type={}, id={}", eventType, eventId);

        switch (eventType) {
            case "payment.succeeded.v1" -> handlePaymentSucceeded(eventId, payload);
            case "tenancy.confirmed.v1" -> handleTenancyConfirmed(eventId, payload);
            case "rental.reservation-confirmed.v1" -> handleReservationConfirmed(eventId, payload);
            case "landlord.verified.v1" -> handleLandlordVerified(eventId, payload);
            default -> log.debug("Notification Service ignoring unhandled event type: {}", eventType);
        }
    }

    private void handleTenancyConfirmed(UUID eventId, JsonNode payload) {
        String tenantId = payload.path("tenantKeycloakId").asString();
        var userProfile = accountClient.getUserProfile(tenantId);
        String tenantEmail = userProfile.email();
        String recipientName = userProfile.fullName() != null ? userProfile.fullName() : "Tenant";

        String startDate = payload.path("startDate").asString();
        String endDate = payload.path("endDate").asString();
        String propertyTitle = payload.hasNonNull("propertyTitle")
                ? payload.path("propertyTitle").asString()
                : "your rented property";

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", recipientName);
        model.put("propertyTitle", propertyTitle);
        model.put("startDate", startDate);
        model.put("endDate", endDate);

        dispatchService.dispatch(eventId, "TENANCY_ACTIVATED", tenantEmail, tenantId, model);
    }

    private void handlePaymentSucceeded(UUID eventId, JsonNode payload) {
        String tenantId = payload.path("tenantKeycloakId").asString();
        var userProfile = accountClient.getUserProfile(tenantId);
        String tenantEmail = userProfile.email();
        String recipientName = userProfile.fullName() != null ? userProfile.fullName() : "Valued Customer";

        String amount = payload.path("amount").asString();
        String currency = payload.path("currency").asString();
        String providerRef = payload.path("providerReference").asString();
        String receiptNumber = payload.path("receiptNumber").asString();

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", recipientName);
        model.put("amount", amount);
        model.put("currency", currency);
        model.put("reservationReference", providerRef);
        model.put("receiptNumber", receiptNumber);

        dispatchService.dispatch(eventId, "PAYMENT_RECEIPT", tenantEmail, tenantId, model);
    }

    private void handleReservationConfirmed(UUID eventId, JsonNode payload) {
        String tenantId = payload.path("tenantKeycloakId").asString();
        var userProfile = accountClient.getUserProfile(tenantId);
        String tenantEmail = userProfile.email();
        String recipientName = userProfile.fullName() != null ? userProfile.fullName() : "Tenant";


        String reservationRef = payload.path("reservationReference").asString();
        String totalAmount = payload.path("totalMoveInAmount").asString();
        String currency = payload.path("currency").asString("NGN");
        String holdDeadline = payload.path("holdDeadline").asString();

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", recipientName);
        model.put("reservationReference", reservationRef);
        model.put("totalAmount", totalAmount);
        model.put("currency", currency);
        model.put("holdDeadline", holdDeadline);

        dispatchService.dispatch(eventId, "RESERVATION_CONFIRMED", tenantEmail, tenantId, model);
    }

    private void handleLandlordVerified(UUID eventId, JsonNode payload) {
        String landlordId = payload.path("landlordKeycloakId").asString();
        var userProfile = accountClient.getUserProfile(landlordId);
        String landlordEmail = userProfile.email();
        String recipientName = userProfile.fullName() != null ? userProfile.fullName() : "Landlord Partner";

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", recipientName);

        dispatchService.dispatch(eventId, "LANDLORD_VERIFIED", landlordEmail, landlordId, model);
    }
}
