package com.lagosrent.rental.application.service;

import com.lagosrent.rental.api.dto.ApplicationResponse;
import com.lagosrent.rental.api.dto.CreateApplicationRequest;
import com.lagosrent.rental.api.dto.DecideApplicationRequest;
import com.lagosrent.rental.application.exception.ResourceNotFoundException;
import com.lagosrent.rental.domain.model.PriceSnapshot;
import com.lagosrent.rental.domain.model.RentalApplication;
import com.lagosrent.rental.domain.model.RentalSaga;
import com.lagosrent.rental.domain.model.Reservation;
import com.lagosrent.rental.infrastructure.client.ListingClient;
import com.lagosrent.rental.infrastructure.messaging.payload.RentalReservationRequestedPayload;
import com.lagosrent.rental.infrastructure.persistence.PriceSnapshotRepository;
import com.lagosrent.rental.infrastructure.persistence.RentalApplicationRepository;
import com.lagosrent.rental.infrastructure.persistence.RentalSagaRepository;
import com.lagosrent.rental.infrastructure.persistence.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.lagosrent.rental.domain.model.ApplicationStatus.*;
import static java.math.BigDecimal.ZERO;
import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalApplicationService {
    private final RentalApplicationRepository applicationRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ReservationRepository reservationRepository;
    private final RentalSagaRepository sagaRepository;
    private final ListingClient listingClient;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApplicationResponse createDraftApplication(String tenantKeycloakId, CreateApplicationRequest request) {
        ListingClient.ListingDetailDto listing = listingClient.getListing(request.listingId());

        RentalApplication app = new RentalApplication(
                request.listingId(),
                tenantKeycloakId,
                listing.landlordKeycloakId()
        );
        app.setEmploymentStatus(request.employmentStatus());
        app.setAnnualIncome(request.annualIncome());
        app.setPreviousLandlordContact(request.previousLandlordContact());

        return ApplicationResponse.from(applicationRepository.save(app));
    }

    /**
     * Submitting application fetches the live listing and locks the immutable PriceSnapshot!
     */
    @Transactional
    public ApplicationResponse submitApplication(UUID applicationId, String tenantKeycloakId) {
        RentalApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalApplication", applicationId));

        if (!app.getTenantKeycloakId().equals(tenantKeycloakId)) {
            throw new IllegalArgumentException("You do not own this application");
        }

        if (app.getStatus() != DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be submitted");
        }

        // Fetch authoritative live listing from Listing Service
        ListingClient.ListingDetailDto listing = listingClient.getListing(app.getListingId());
        if (!"PUBLISHED".equals(listing.status())) {
            throw new IllegalStateException("Application cannot be submitted because this listing is no longer available. Current status: " + listing.status());
        }

        // Extract individual fee line amounts for the snapshot
        BigDecimal cautionDeposit = ZERO;
        BigDecimal serviceCharge = ZERO;
        BigDecimal legalFee = ZERO;

        if (listing.fees() != null) {
            for (ListingClient.ListingFeeDto fee : listing.fees()) {
                if ("CAUTION_DEPOSIT".equalsIgnoreCase(fee.feeType())) {
                    cautionDeposit = fee.amount();
                } else if ("SERVICE_CHARGE".equalsIgnoreCase(fee.feeType())) {
                    serviceCharge = fee.amount();
                } else if ("LEGAL_AND_COUNSEL".equalsIgnoreCase(fee.feeType()) || "LEGAL_AND_AGREEMENT".equalsIgnoreCase(fee.feeType())) {
                    legalFee = fee.amount();
                }
            }
        }

        try {
            String feeBreakdownJson = objectMapper.writeValueAsString(listing.fees());

            // Create and persist the Immutable PriceSnapshot
            PriceSnapshot snapshot = new PriceSnapshot(
                    app,
                    listing.id(),
                    listing.version(),
                    listing.rentAmount(),
                    cautionDeposit,
                    serviceCharge,
                    legalFee,
                    listing.moveInTotal(),
                    feeBreakdownJson
            );
            priceSnapshotRepository.save(snapshot);
            app.setPriceSnapshot(snapshot);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to serialize fee breakdown for price snapshot", e);
        }

        app.setStatus(SUBMITTED);
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    /**
     * Landlord accepts application:
     * 1. Updates status to ACCEPTED
     * 2. Writes RentalSaga state in rental_db
     * 3. Creates Reservation record in rental_db
     * 4. Atomically records 'rental.reservation-requested.v1' into outbox_events!
     */
    @Transactional
    public ApplicationResponse acceptApplication(UUID applicationId, String landlordKeycloakId) {
        RentalApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalApplication", applicationId));

        if (!app.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own the listing for this application");
        }

        if (app.getStatus() != SUBMITTED && app.getStatus() != UNDER_REVIEW) {
            throw new IllegalStateException("Only SUBMITTED or UNDER_REVIEW applications can be accepted");
        }

        app.setStatus(ACCEPTED);
        RentalApplication savedApp = applicationRepository.save(app);

        // Generate unique reservation reference
        String reservationRef = "RES-" + randomUUID().toString().substring(0, 8).toUpperCase();
        Instant holdDeadline = Instant.now().plus(48, ChronoUnit.HOURS); // 48-hour hold window

        // Create reservation record
        Reservation reservation = new Reservation(savedApp, savedApp.getListingId(), reservationRef, holdDeadline);
        reservationRepository.save(reservation);

        // Persist Saga Orchestration row
        RentalSaga saga = new RentalSaga(savedApp, savedApp.getListingId(), reservationRef);
        sagaRepository.save(saga);

        // Atomically record reservation command in the Outbox
        outboxService.recordEvent(
                "rental_saga",
                saga.getId().toString(),
                1L,
                "rental.reservation-requested.v1",
                new RentalReservationRequestedPayload(savedApp.getId(),
                        savedApp.getListingId(),
                        savedApp.getTenantKeycloakId(),
                        reservationRef,
                        holdDeadline,
                        Instant.now()
                ),
                null,
                null
        );
        log.info("Application accepted and saga started: appId={}, reservationRef={}", savedApp.getId(), reservationRef);
        return ApplicationResponse.from(savedApp);
    }

    @Transactional
    public ApplicationResponse rejectApplication(UUID applicationId, String landlordKeycloakId, DecideApplicationRequest request) {
        RentalApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalApplication", applicationId));

        if (!app.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own the listing for this application");
        }

        app.setStatus(REJECTED);
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID applicationId) {
        RentalApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalApplication", applicationId));
        return ApplicationResponse.from(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getTenantApplications(String tenantKeycloakId) {
        return applicationRepository.findByTenantKeycloakId(tenantKeycloakId).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getListingApplications(UUID listingId, String landlordKeycloakId) {
        return applicationRepository.findByListingId(listingId).stream()
                .filter(app -> app.getLandlordKeycloakId().equals(landlordKeycloakId))
                .map(ApplicationResponse::from)
                .toList();
    }
}
