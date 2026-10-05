package com.lagosrent.search.infrastructure.messaging;

import com.lagosrent.search.domain.model.ProcessedMessage;
import com.lagosrent.search.domain.model.SearchListing;
import com.lagosrent.search.infrastructure.messaging.payload.ListingPublishedPayload;
import com.lagosrent.search.infrastructure.persistence.ProcessedMessageRepository;
import com.lagosrent.search.infrastructure.persistence.SearchListingRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListingProjectionConsumer {
    public static final String CONSUMER_NAME = "search.listing-projection";
    public static final String QUEUE_NAME = "search.listing-projection.v1";

    private final SearchListingRepository searchListingRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = QUEUE_NAME, ackMode = "MANUAL")
    @Transactional
    public void onListingEvent(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 1. Deserialize the Event Envelope
            EventEnvelope<?> envelope = objectMapper.readValue(message.getBody(), EventEnvelope.class);
            log.info("Received event: id={}, type={}, aggregateId={}",
                    envelope.eventId(), envelope.eventType(), envelope.aggregateId());

            // 2. Idempotency Check (Skip if duplicate event)
            if (processedMessageRepository.existsByIdEventIdAndIdConsumerName(envelope.eventId(), CONSUMER_NAME)) {
                log.warn("Duplicate message detected (eventId={}, consumer={}). Skipping.", envelope.eventId(), CONSUMER_NAME);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. Process event types
            if ("listing.published.v1".equals(envelope.eventType())) {
                ListingPublishedPayload payload = objectMapper.convertValue(
                        envelope.payload(), ListingPublishedPayload.class);

                upsertListingProjection(payload, envelope.aggregateVersion());
            }

            // 4. Record to inbox inside the same transaction
            ProcessedMessage inboxRecord = new ProcessedMessage(
                    envelope.eventId(),
                    CONSUMER_NAME,
                    envelope.eventType()
            );
            processedMessageRepository.save(inboxRecord);

            // 5. Manual ACK back to RabbitMQ
            channel.basicAck(deliveryTag, false);
            log.info("Successfully updated search projection and ACKed event id={}", envelope.eventId());
        }
        catch (Exception ex) {
            log.error("Failed to process event deliveryTag={}: {}", deliveryTag, ex.getMessage(), ex);
            // Requeue=false routes the failed message to Dead-Letter Queue
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void upsertListingProjection(ListingPublishedPayload payload, Long eventVersion) {
        Optional<SearchListing> existingOpt = searchListingRepository.findById(payload.listingId());

        // Discard event if current projection is newer or equal
        if (existingOpt.isPresent()) {
            SearchListing existing = existingOpt.get();
            if (existing.getListingVersion() >= eventVersion) {
                log.warn("Stale event ignored for listingId={}. Existing version={}, event version={}",
                        payload.listingId(), existing.getListingVersion(), eventVersion);
                return;
            }
        }

        SearchListing projection = existingOpt.orElseGet(SearchListing::new);
        projection.setListingId(payload.listingId());
        projection.setListingVersion(eventVersion);
        projection.setLandlordKeycloakId(payload.landlordKeycloakId());
        projection.setTitle(payload.title());
        projection.setDescription(payload.description());
        projection.setRentAmount(payload.rentAmount());
        projection.setRentPeriod(payload.rentPeriod());
        projection.setMoveInTotal(payload.moveInTotal());
        projection.setAvailableFrom(payload.availableFrom());
        projection.setState(payload.state());
        projection.setLga(payload.lga());
        projection.setArea(payload.area());
        projection.setLandmark(payload.landmark());
        projection.setLatitude(payload.latitude());
        projection.setLongitude(payload.longitude());
        projection.setUnitType(payload.unitType());
        projection.setBedrooms(payload.bedrooms());
        projection.setBathrooms(payload.bathrooms());
        projection.setToilets(payload.toilets());
        projection.setSizeSqMeters(payload.sizeSqMeters());
        projection.setFloorNumber(payload.floorNumber());
        projection.setStatus("PUBLISHED");

        // Primary image
        if (payload.media() != null && !payload.media().isEmpty()) {
            payload.media().stream()
                    .filter(ListingPublishedPayload.MediaItemPayload::isPrimary)
                    .findFirst()
                    .ifPresentOrElse(
                            m -> projection.setPrimaryImageUrl(m.s3Key()),
                            () -> projection.setPrimaryImageUrl(payload.media().getFirst().s3Key())
                    );
        }

        Instant now = Instant.now();
        if (projection.getPublishedAt() == null) {
            projection.setPublishedAt(now);
        }
        projection.setUpdatedAt(now);

        searchListingRepository.save(projection);
        log.info("Saved search projection for listingId={}, title={}", payload.listingId(), payload.title());
    }
}
