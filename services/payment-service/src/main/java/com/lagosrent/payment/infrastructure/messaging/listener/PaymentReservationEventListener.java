package com.lagosrent.payment.infrastructure.messaging.listener;

import com.lagosrent.payment.domain.model.Payable;
import com.lagosrent.payment.infrastructure.persistence.PayableRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.UUID.fromString;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReservationEventListener {
    private final PayableRepository payableRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "payment.events.v1", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handlePaymentEvents(Message rawMessage, Channel channel) throws Exception {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        String body = new String(rawMessage.getBody());

        try {
            JsonNode root = objectMapper.readTree(body);
            String eventType = root.path("eventType").asString();
            JsonNode payloadNode = root.path("payload");

            log.info("Payment Service received event: type={}", eventType);
            if ("rental.reservation-confirmed.v1".equals(eventType)) {
                handleReservationConfirmed(payloadNode);
            }

            channel.basicAck(deliveryTag, false);
        }
        catch (Exception ex) {
            log.error("Failed to process event in Payment Service: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handleReservationConfirmed(JsonNode payload) {
        UUID applicationId = fromString(payload.path("applicationId").asString());

        // Idempotency check
        if (payableRepository.findByApplicationId(applicationId).isPresent()) {
            log.info("Payable already exists for application: {}", applicationId);
            return;
        }

        UUID listingId = fromString(payload.path("listingId").asString());
        String tenantKeycloakId = payload.path("tenantKeycloakId").asString();
        String landlordKeycloakId = payload.path("landlordKeycloakId").asString();
        String reservationRef = payload.path("reservationReference").asString();
        BigDecimal totalAmount = new BigDecimal(payload.path("totalMoveInAmount").asString());
        String currency = payload.path("currency").asString("NGN");
        String holdDeadlineStr = payload.path("holdDeadline").asString();

        Instant dueDate = holdDeadlineStr.isBlank() ? now().plusSeconds(172800) : parse(holdDeadlineStr);

        Payable payable = new Payable(
                applicationId,
                listingId,
                tenantKeycloakId,
                landlordKeycloakId,
                reservationRef,
                totalAmount,
                currency,
                dueDate
        );
        Payable saved = payableRepository.save(payable);
        log.info("Auto-provisioned Payable from Saga event: payableId={}, ref={}, amount={} {}",
                saved.getId(), reservationRef, totalAmount, currency);
    }
}
