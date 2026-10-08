package com.lagosrent.listing.infrastructure.messaging.listener;

import com.lagosrent.listing.application.service.OutboxService;
import com.lagosrent.listing.infrastructure.messaging.payload.ListingReservationRejectedPayload;
import com.lagosrent.listing.infrastructure.messaging.payload.ListingReservedPayload;
import com.lagosrent.listing.infrastructure.persistence.ListingRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.UUID.fromString;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListingSagaEventListener {
    private final ListingRepository listingRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "listing.events.v1", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handleListingEvents(Message rawMessage, Channel channel) throws Exception {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        String body = new String(rawMessage.getBody());

        try {
            JsonNode root = objectMapper.readTree(body);
            String eventType = root.path("eventType").asString();
            JsonNode payloadNode = root.path("payload");

            log.info("Listing Service received event: type={}", eventType);

            switch (eventType) {
                case "rental.reservation-requested.v1" -> handleReservationRequested(payloadNode);
                case "listing.release-requested.v1" -> handleReleaseRequested(payloadNode);
                case "tenancy.confirmed.v1" -> handleTenancyConfirmed(payloadNode);
                default -> log.debug("Unhandled event type in listing queue: {}", eventType);
            }

            channel.basicAck(deliveryTag, false);
        }
        catch (Exception ex) {
            log.error("Error processing event in Listing Service: {}", ex.getMessage(), ex);
            // In production, reject and route to DLQ if unrecoverable
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handleReservationRequested(JsonNode payload) {
        UUID listingId = fromString(payload.path("listingId").asString());
        String reservationRef = payload.path("reservationReference").asString();
        String holdDeadlineStr = payload.path("holdDeadline").asString();
        Instant holdDeadline = holdDeadlineStr.isBlank() ? now().plusSeconds(172800) : parse(holdDeadlineStr);

        // Atomic lock attempt
        int rowsUpdated = listingRepository.atomicallyReserveListing(listingId, now());

        if (rowsUpdated > 0) {
            log.info("Listing successfully locked: listingId={}, ref={}", listingId, reservationRef);

            // Emit listing.reserved.v1 via Outbox
            outboxService.recordEvent(
                    "listing",
                    listingId.toString(),
                    1L,
                    "listing.reserved.v1",
                    new ListingReservedPayload(listingId, reservationRef, holdDeadline, Instant.now()),
                    null,
                    null
            );
        } else {
            log.warn("Listing contention! Listing is NOT available for reservation: listingId={}", listingId);

            // Emit listing.reservation-rejected.v1 via Outbox
            outboxService.recordEvent(
                    "listing",
                    listingId.toString(),
                    1L,
                    "listing.reservation-rejected.v1",
                    new ListingReservationRejectedPayload(
                            listingId,
                            reservationRef,
                            "Listing is already reserved, rented, or not published",
                            Instant.now()
                    ),
                    null,
                    null
            );
        }
    }

    private void handleReleaseRequested(JsonNode payload) {
        UUID listingId = fromString(payload.path("listingId").asString());
        String reservationRef = payload.path("reservationReference").asString();
        String reason = payload.path("reason").asString();

        log.info("Compensating action: releasing listing lock: listingId={}, reason={}", listingId, reason);
        int rows = listingRepository.atomicallyReleaseListing(listingId, now());
        if (rows > 1) {
            log.info("Listing released back to PUBLISHED: listingId={}", listingId);
        }
    }

    private void handleTenancyConfirmed(JsonNode payload) {
        UUID listingId = UUID.fromString(payload.path("listingId").asText());
        log.info("Tenancy finalized: marking listing as RENTED: listingId={}", listingId);
        listingRepository.atomicallyMarkRented(listingId, Instant.now());
    }
}
