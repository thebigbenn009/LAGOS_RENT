package com.lagosrent.listing.application.service;

import com.lagosrent.listing.api.dto.*;
import com.lagosrent.listing.application.exception.ResourceNotFoundException;
import com.lagosrent.listing.domain.model.*;
import com.lagosrent.listing.infrastructure.messaging.payload.ListingPublishedPayload;
import com.lagosrent.listing.infrastructure.persistence.ListingMediaRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingRepository;
import com.lagosrent.listing.infrastructure.persistence.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final ListingMediaRepository listingMediaRepository;
    private final OutboxService outboxService;

    @Transactional
    public ListingResponse createListing(UUID unitId, String landlordKeycloakId, CreateListingRequest request) {
        RentalUnit unit = rentalUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalUnit", unitId));

        if (!unit.getProperty().getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own the property for this rental unit");
        }

        Listing listing = new Listing(
                unit,
                landlordKeycloakId,
                request.title(),
                request.description(),
                request.rentAmount(),
                request.availableFrom()
        );

        if (request.rentPeriod() != null) {
            listing.setRentPeriod(request.rentPeriod());
        }

        // Add fee lines if provided
        if (request.fees() != null) {
            for (ListingFeeDto feeDto : request.fees()) {
                ListingFee fee = new ListingFee(
                        listing,
                        feeDto.feeType(),
                        feeDto.amount(),
                        feeDto.recipient(),
                        feeDto.frequency() != null ? feeDto.frequency() : FeeFrequency.ONE_OFF,
                        Boolean.TRUE.equals(feeDto.isRefundable()),
                        feeDto.explanation()
                );
                listing.getFees().add(fee);
            }
        }

        recalculateMoveInTotal(listing);
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse updateListing(UUID listingId, String landlordKeycloakId, UpdateListingRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.CHANGES_REQUESTED) {
            throw new IllegalStateException("Only DRAFT or CHANGES_REQUESTED listings can be edited");
        }

        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setRentAmount(request.rentAmount());
        if (request.rentPeriod() != null) listing.setRentPeriod(request.rentPeriod());
        listing.setAvailableFrom(request.availableFrom());

        // Update fee lines
        listing.getFees().clear();
        if (request.fees() != null) {
            for (ListingFeeDto feeDto : request.fees()) {
                ListingFee fee = new ListingFee(
                        listing,
                        feeDto.feeType(),
                        feeDto.amount(),
                        feeDto.recipient(),
                        feeDto.frequency() != null ? feeDto.frequency() : FeeFrequency.ONE_OFF,
                        Boolean.TRUE.equals(feeDto.isRefundable()),
                        feeDto.explanation()
                );
                listing.getFees().add(fee);
            }
        }
        recalculateMoveInTotal(listing);
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse submitForReview(UUID listingId, String landlordKeycloakId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.CHANGES_REQUESTED) {
            throw new IllegalStateException("Listing is not in a submittable state");
        }

        if (listing.getRentAmount() == null || listing.getRentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Rent amount is required to submit listing");
        }

        recalculateMoveInTotal(listing);
        listing.setStatus(ListingStatus.SUBMITTED_FOR_REVIEW);
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse publishListing(UUID listingId, String landlordKeycloakId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        if (listing.getStatus() != ListingStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED listings can be published");
        }

        listing.setStatus(ListingStatus.PUBLISHED);
        Listing savedListing = listingRepository.saveAndFlush(listing);

        // Record outbox event atomically in the exact same transaction
        ListingPublishedPayload payload = toListingPublishedPayload(savedListing);
        outboxService.recordEvent(
                "listing",
                savedListing.getId().toString(),
                savedListing.getVersion(),
                "listing.published.v1",
                payload,
                null,
                null
        );

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingResponse getListingById(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));
        return ListingResponse.from(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getLandlordListings(String landlordKeycloakId) {
        return listingRepository.findByLandlordKeycloakId(landlordKeycloakId)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    @Transactional
    public ListingMediaDto addMedia(UUID listingId, String landlordKeycloakId, ListingMediaDto mediaDto) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        ListingMedia media = new ListingMedia(
                listing,
                mediaDto.mediaType(),
                mediaDto.s3Key(),
                mediaDto.filename(),
                mediaDto.orderIndex() != null ? mediaDto.orderIndex() : 0,
                Boolean.TRUE.equals(mediaDto.isPrimary())
        );
        media.setCaption(mediaDto.caption());
        return ListingMediaDto.from(listingMediaRepository.save(media));
    }

    private void recalculateMoveInTotal(Listing listing) {
        BigDecimal total = listing.getRentAmount() != null ? listing.getRentAmount() : BigDecimal.ZERO;
        if (listing.getFees() != null) {
            for (ListingFee fee : listing.getFees()) {
                // Initial move in cost includes one-off upfront fees
                if (fee.getFrequency() == FeeFrequency.ONE_OFF) {
                    total = total.add(fee.getAmount());
                }
            }
        }
        listing.setMoveInTotal(total);
    }

    private ListingPublishedPayload toListingPublishedPayload(Listing listing) {
        RentalUnit unit = listing.getRentalUnit();
        Property property = unit.getProperty();

        List<ListingPublishedPayload.FeeItemPayload> feePayloads = listing.getFees().stream()
                .map(fee -> new ListingPublishedPayload.FeeItemPayload(
                        fee.getFeeType().name(),
                        fee.getAmount(),
                        fee.getRecipient().name(),
                        fee.getFrequency().name(),
                        fee.isRefundable(),
                        fee.getExplanation()
                ))
                .toList();

        List<ListingPublishedPayload.MediaItemPayload> mediaPayloads = listing.getMedia().stream()
                .map(m -> new ListingPublishedPayload.MediaItemPayload(
                        m.getMediaType().name(),
                        m.getS3Key(),
                        m.getFilename(),
                        m.getOrderIndex(),
                        m.getCaption(),
                        m.isPrimary()
                ))
                .toList();

        return new ListingPublishedPayload(
                listing.getId(),
                listing.getLandlordKeycloakId(),
                property.getId(),
                unit.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getRentAmount(),
                listing.getRentPeriod(),
                listing.getMoveInTotal(),
                listing.getAvailableFrom(),
                property.getState(),
                property.getLga(),
                property.getArea(),
                property.getLandmark(),
                property.getLatitude(),
                property.getLongitude(),
                unit.getUnitType().name(),
                unit.getBedrooms(),
                unit.getBathrooms(),
                unit.getToilets(),
                unit.getSizeSqMeters(),
                unit.getFloorNumber(),
                feePayloads,
                mediaPayloads
        );
    }
}
