package com.lagosrent.rental.infrastructure.infrastructure.messaging.listener;

import com.lagosrent.rental.application.service.OutboxService;
import com.lagosrent.rental.domain.model.RentalApplication;
import com.lagosrent.rental.domain.model.RentalSaga;
import com.lagosrent.rental.domain.model.SagaStep;
import com.lagosrent.rental.domain.model.Tenancy;
import com.lagosrent.rental.infrastructure.messaging.payload.ListingReleaseRequestedPayload;
import com.lagosrent.rental.infrastructure.messaging.payload.RentalReservationConfirmedPayload;
import com.lagosrent.rental.infrastructure.messaging.payload.TenancyConfirmedPayload;
import com.lagosrent.rental.infrastructure.persistence.RentalApplicationRepository;
import com.lagosrent.rental.infrastructure.persistence.RentalSagaRepository;
import com.lagosrent.rental.infrastructure.persistence.TenancyRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static com.lagosrent.rental.domain.model.ApplicationStatus.REJECTED;
import static com.lagosrent.rental.domain.model.SagaStatus.*;
import static com.lagosrent.rental.domain.model.SagaStatus.PAYMENT_PENDING;
import static com.lagosrent.rental.domain.model.SagaStep.*;
import static java.time.Instant.now;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalSagaEventListener {
    private final RentalSagaRepository sagaRepository;
    private final RentalApplicationRepository applicationRepository;
    private final TenancyRepository tenancyRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "rental.workflow.v1", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void handleRentalWorkflowEvents(Message rawMessage, Channel channel) throws Exception {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        String body = new String(rawMessage.getBody());

        try {
            JsonNode root = objectMapper.readTree(body);
            String eventType = root.path("eventType").asString();
            JsonNode payloadNode = root.path("payload");

            log.info("Rental Saga Orchestrator received event: type={}", eventType);

            switch (eventType) {
                case "listing.reserved.v1" -> handleListingReserved(payloadNode);
                case "listing.reservation-rejected.v1" -> handleListingReservationRejected(payloadNode);
                case "payment.succeeded.v1" -> handlePaymentSucceeded(payloadNode);
                case "payment.failed.v1" -> handlePaymentFailed(payloadNode);
                default -> log.debug("Unhandled event in rental workflow queue: {}", eventType);
            }
            channel.basicAck(deliveryTag, true);

        }
        catch (Exception ex) {
            log.error("Failed to process event in Rental Saga Orchestrator: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void handleListingReserved(JsonNode payload) {
        String reservationRef = payload.path("reservationReference").asString();

        RentalSaga saga = sagaRepository.findByReservationReference(reservationRef)
                .orElse(null);

        if (saga == null) {
            log.warn("No saga found for reservationRef: {}", reservationRef);
            return;
        }
        saga.setStatus(PAYMENT_PENDING);
        saga.setCurrentStep(SagaStep.PAYMENT_PENDING);
        sagaRepository.save(saga);

        RentalApplication app = saga.getApplication();

        // Emit rental.reservation-confirmed.v1 via Outbox. Payment service consumes this!
        outboxService.recordEvent(
                "rental_saga",
                saga.getId().toString(),
                2L,
                "rental.reservation-confirmed.v1",
                new RentalReservationConfirmedPayload(
                        app.getId(),
                        app.getListingId(),
                        app.getTenantKeycloakId(),
                        app.getLandlordKeycloakId(),
                        reservationRef,
                        app.getPriceSnapshot().getTotalMoveInAmount(),
                        app.getPriceSnapshot().getCurrency(),
                        now().plusSeconds(172800), // 48-hr hold deadline
                        now()
                ),
                null,
                null
        );
        log.info("Saga advanced to PAYMENT+PENDING: sagaId={}. ref={}", saga.getId(), reservationRef);
    }

    private void handleListingReservationRejected(JsonNode payload) {
        String reservationRef = payload.path("reservationReference").asString();
        String reason = payload.path("reason").asString();

        sagaRepository.findByReservationReference(reservationRef).ifPresent(saga -> {
            saga.setStatus(FAILED);
            saga.setCurrentStep(FAILED_RESERVATION_REJECTED);
            sagaRepository.save(saga);

            RentalApplication app = saga.getApplication();
            app.setStatus(REJECTED);
            applicationRepository.save(app);

            log.warn("Saga FAILED due to inventory contention: sagaId={}, reason={}", saga.getId(), reason);

        });
    }

    private void handlePaymentSucceeded(JsonNode payload) {
        UUID applicationId = UUID.fromString(payload.path("applicationId").asString());

        RentalSaga saga = sagaRepository.findByApplicationId(applicationId)
                .orElse(null);

        if (saga == null) {
            log.warn("Payment succeeded for unknown saga: applicationId={}", applicationId);
            return;
        }

        // Idempotency check
        if (saga.getStatus() == COMPLETED) {
            log.info("Saga already completed: sagaId={}", saga.getId());
            return;
        }

        RentalApplication app = saga.getApplication();

        // 1. Activate Tenancy
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        Tenancy tenancy = new Tenancy(
                app,
                app.getListingId(),
                app.getTenantKeycloakId(),
                app.getLandlordKeycloakId(),
                app.getPriceSnapshot().getRentAmount(),
                startDate,
                endDate
        );
        tenancyRepository.save(tenancy);

        // 2. Mark Saga COMPLETED
        saga.setStatus(COMPLETED);
        saga.setCurrentStep(COMPLETED_TENANCY_ACTIVE);
        sagaRepository.save(saga);

        // 3. Emit tenancy.confirmed.v1 via Outbox. Listing Service marks unit RENTED
        outboxService.recordEvent(
                "rental_saga",
                saga.getId().toString(),
                3L,
                "tenancy.confirmed.v1",
                new TenancyConfirmedPayload(
                        tenancy.getId(),
                        app.getId(),
                        app.getListingId(),
                        app.getTenantKeycloakId(),
                        app.getLandlordKeycloakId(),
                        startDate,
                        endDate,
                        tenancy.getAnnualRent(),
                        now()
                ),
                null,
                null
        );
        log.info("Tenancy officially activated! sagaId={}, tenancyId={}", saga.getId(), tenancy.getId());
    }

    private void handlePaymentFailed(JsonNode payload) {
        UUID applicationId = UUID.fromString(payload.path("applicationId").asString());

        sagaRepository.findByApplicationId(applicationId).ifPresent(saga -> {
            saga.setStatus(FAILED);
            saga.setCurrentStep(COMPENSATING_RELEASE_REQUESTED);
            sagaRepository.save(saga);

            // Emit Compensating command
            outboxService.recordEvent(
                    "rental-saga",
                    saga.getId().toString(),
                    4L,
                    "listing.release-requested.v1",
                    new ListingReleaseRequestedPayload(
                            saga.getListingId(),
                            saga.getReservationReference(),
                            "PAYMENT_FAILED",
                            now()
                    ),
                    null,
                    null
            );
            log.info("Compensating action triggered: requested listing release for sagaId={}", saga.getId());
        });
    }
}
