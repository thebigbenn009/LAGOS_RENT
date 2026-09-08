package com.lagosrent.listing.application.service;

import com.lagosrent.listing.api.dto.ListingResponse;
import com.lagosrent.listing.application.exception.ResourceNotFoundException;
import com.lagosrent.listing.domain.model.*;
import com.lagosrent.listing.infrastructure.persistence.AdminAuditRecordRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminListingService {

    private final ListingRepository listingRepository;
    private final ListingReviewRepository reviewRepository;
    private final AdminAuditRecordRepository auditRecordRepository;

    @Transactional(readOnly = true)
    public Page<ListingResponse> getReviewQueue(Pageable pageable) {
        return listingRepository.findByStatus(ListingStatus.SUBMITTED_FOR_REVIEW, pageable)
                .map(ListingResponse::from);
    }

    @Transactional
    public ListingResponse approveListing(UUID listingId, String adminKeycloakId, String reason, Long expectedVersion) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getVersion().equals(expectedVersion)) {
            throw new IllegalStateException("Listing has been modified since it was loaded. Refresh and try again.");
        }

        if (listing.getStatus() != ListingStatus.SUBMITTED_FOR_REVIEW) {
            throw new IllegalStateException("Only SUBMITTED_FOR_REVIEW listings can be approved");
        }

        // Approve any other fee lines
        for (ListingFee fee : listing.getFees()) {
            if (fee.getFeeType() == FeeType.OTHER) {
                fee.setApprovedByAdmin(true);
            }
        }

        String previousState = listing.getStatus().name();
        listing.setStatus(ListingStatus.APPROVED);

        reviewRepository.save(new ListingReview(
                listing,
                adminKeycloakId,
                ReviewDecision.APPROVED,
                reason,
                listing.getVersion()
        ));

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "APPROVE_LISTING",
                "Listing",
                listingId.toString(),
                reason,
                previousState,
                ListingStatus.APPROVED.name()
        ));

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse requestChanges(UUID listingId, String adminKeycloakId, String reason, Long expectedVersion) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getVersion().equals(expectedVersion)) {
            throw new IllegalStateException("Listing has been modified since it was loaded");
        }

        String previousState = listing.getStatus().name();
        listing.setStatus(ListingStatus.CHANGES_REQUESTED);

        reviewRepository.save(new ListingReview(
                listing,
                adminKeycloakId,
                ReviewDecision.CHANGES_REQUESTED,
                reason,
                listing.getVersion()
        ));

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "REQUEST_CHANGES_LISTING",
                "Listing",
                listingId.toString(),
                reason,
                previousState,
                ListingStatus.CHANGES_REQUESTED.name()
        ));

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse rejectingListing(UUID listingId, String adminKeycloakId, String reason, Long expectedVersion) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getVersion().equals(expectedVersion)) {
            throw new IllegalStateException("Listing has been modified since it was loaded");
        }

        String previousState = listing.getStatus().name();
        listing.setStatus(ListingStatus.REJECTED);

        reviewRepository.save(new ListingReview(
                listing,
                adminKeycloakId,
                ReviewDecision.REJECTED,
                reason,
                listing.getVersion()
        ));

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "REJECT_LISTING",
                "Listing",
                listingId.toString(),
                reason,
                previousState,
                ListingStatus.REJECTED.name()
        ));

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse suspendListing(UUID listingId, String adminKeycloakId, String reason) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        String previousState = listing.getStatus().name();
        listing.setStatus(ListingStatus.SUSPENDED);

        auditRecordRepository.save(new AdminAuditRecord(
                adminKeycloakId,
                "SUSPEND_LISTING",
                "Listing",
                listingId.toString(),
                reason,
                previousState,
                ListingStatus.SUSPENDED.name()
        ));

        return ListingResponse.from(listingRepository.save(listing));
    }
}
