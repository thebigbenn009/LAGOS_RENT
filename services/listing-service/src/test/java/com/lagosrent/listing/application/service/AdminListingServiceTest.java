package com.lagosrent.listing.application.service;

import com.lagosrent.listing.api.dto.ListingResponse;
import com.lagosrent.listing.application.exception.ResourceNotFoundException;
import com.lagosrent.listing.domain.model.*;
import com.lagosrent.listing.infrastructure.persistence.AdminAuditRecordRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lagosrent.listing.domain.model.FeeFrequency.ONE_OFF;
import static com.lagosrent.listing.domain.model.FeeRecipient.PROPERTY_MANAGER;
import static com.lagosrent.listing.domain.model.FeeType.OTHER;
import static com.lagosrent.listing.domain.model.ListingStatus.REJECTED;
import static com.lagosrent.listing.domain.model.ListingStatus.SUBMITTED_FOR_REVIEW;
import static com.lagosrent.listing.domain.model.ReviewDecision.APPROVED;
import static com.lagosrent.listing.domain.model.ReviewDecision.CHANGES_REQUESTED;
import static com.lagosrent.listing.domain.model.UnitType.APARTMENT;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminListingService — Review Decisions, Optimistic Concurrency & Audit Trail Unit Tests")
public class AdminListingServiceTest {

    // Verified seeded identities
    private static final String LANDLORD_KEYCLOAK_ID = "818746e8-5729-4aa5-91fc-a99199dbd2aa";
    private static final String ADMIN_KEYCLOAK_ID = "admin_test";

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingReviewRepository reviewRepository;

    @Mock
    private AdminAuditRecordRepository auditRecordRepository;

    @Captor
    private ArgumentCaptor<ListingReview> reviewCaptor;

    @Captor
    private ArgumentCaptor<AdminAuditRecord> auditCaptor;

    private AdminListingService adminListingService;

    private UUID listingId;
    private Listing listing;

    @BeforeEach
    void setUp() {
        adminListingService = new AdminListingService(
                listingRepository,
                reviewRepository,
                auditRecordRepository
        );

        listingId = randomUUID();

        Property property = new Property(
                LANDLORD_KEYCLOAK_ID,
                "Admiralty Heights",
                "Eti-Osa",
                "Lekki Phase 1",
                "Plot 14 Admiralty Way",
                "Beside Daytona Supermarket"
        );
        ReflectionTestUtils.setField(property, "id", randomUUID());

        RentalUnit unit = new RentalUnit(property, "Flat 2B", APARTMENT, 3, 3, 4);
        ReflectionTestUtils.setField(unit, "id", randomUUID());

        listing = new Listing(
                unit,
                LANDLORD_KEYCLOAK_ID,
                "Serviced 3-Bedroom Apartment in Lekki Phase 1",
                "24/7 power and treated water",
                new BigDecimal("4500000.00"),
                LocalDate.of(2026, 12, 1)
        );
        listing.setStatus(SUBMITTED_FOR_REVIEW);
        ReflectionTestUtils.setField(listing, "id", listingId);
        ReflectionTestUtils.setField(listing, "version", 2L);
    }

    // =========================================================================
    // 1. getReviewQueue
    // =========================================================================
    @Nested
    @DisplayName("getReviewQueue")
    class GetReviewQueue {
        @Test
        @DisplayName("returns paginated SUBMITTED_FOR_REVIEW listings mapped to ListingResponse")
        void returnsPaginatedSubmittedListings() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            given(listingRepository.findByStatus(SUBMITTED_FOR_REVIEW, pageable))
                    .willReturn(new PageImpl<>(List.of(listing), pageable, 1));

            // when
            Page<ListingResponse> page = adminListingService.getReviewQueue(pageable);

            // then
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent().getFirst().id()).isEqualTo(listingId);
            assertThat(page.getContent().getFirst().status()).isEqualTo(SUBMITTED_FOR_REVIEW);
        }
    }

    // =========================================================================
    // 2. approveListing
    // =========================================================================
    @Nested
    @DisplayName("approveListing")
    class ApproveListing {

        @Test
        @DisplayName("transitions SUBMITTED_FOR_REVIEW listing to APPROVED, approves OTHER fees, and records ListingReview and AdminAuditRecord")
        void approvesSubmittedListingAndApprovesOtherFeesAndRecordsAudit() {
            // given: an unapproved OTHER fee attached to the submitted listing
            ListingFee otherFee = new ListingFee(
                    listing,
                    OTHER,
                    new BigDecimal("75000.00"),
                    PROPERTY_MANAGER,
                    ONE_OFF,
                    false,
                    "Estate biometric access card"
            );
            listing.getFees().add(otherFee);
            assertThat(otherFee.isApprovedByAdmin()).isFalse();

            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));

            String reason = "Deed of assignment and OTHER fee breakdown verified.";

            // when
            ListingResponse response = adminListingService.approveListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    reason,
                    2L
            );

            // then: 1. Status transitions to APPROVED and OTHER fee is now approvedByAdmin = true
            assertThat(response.status()).isEqualTo(ListingStatus.APPROVED);
            assertThat(otherFee.isApprovedByAdmin()).isTrue();
            assertThat(response.fees().getFirst().approvedByAdmin()).isTrue();

            // 2. ListingReview recorded with exact version and reason
            then(reviewRepository).should().save(reviewCaptor.capture());
            ListingReview review = reviewCaptor.getValue();
            assertThat(review.getListing()).isSameAs(listing);
            assertThat(review.getAdminKeycloakId()).isEqualTo(ADMIN_KEYCLOAK_ID);
            assertThat(review.getDecision()).isEqualTo(APPROVED);
            assertThat(review.getReason()).isEqualTo(reason);
            assertThat(review.getListingVersion()).isEqualTo(2L);

            // 3. Immutable AdminAuditRecord recorded with before/after states
            then(auditRecordRepository).should().save(auditCaptor.capture());
            AdminAuditRecord audit = auditCaptor.getValue();
            assertThat(audit.getAdminKeycloakId()).isEqualTo(ADMIN_KEYCLOAK_ID);
            assertThat(audit.getActionType()).isEqualTo("APPROVE_LISTING");
            assertThat(audit.getEntityType()).isEqualTo("Listing");
            assertThat(audit.getEntityId()).isEqualTo(listingId.toString());
            assertThat(audit.getReason()).isEqualTo(reason);
            assertThat(audit.getPreviousState()).isEqualTo("SUBMITTED_FOR_REVIEW");
            assertThat(audit.getNewState()).isEqualTo("APPROVED");

        }

        @Test
        @DisplayName("rejects approval with IllegalStateException and writes nothing when expectedVersion does not match current version")
        void rejectsApprovalOnOptimisticVersionMismatch() {
            // given: listing is at version 2L, but admin supplies stale expectedVersion = 1L
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.approveListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Approved",
                    1L
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Listing has been modified since it was loaded");

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());
        }

        @ParameterizedTest(name = "rejects approving listing when status is {0}")
        @EnumSource(value = ListingStatus.class, names = "SUBMITTED_FOR_REVIEW", mode = EXCLUDE)
        @DisplayName("rejects approval from every status other than SUBMITTED_FOR_REVIEW and writes nothing")
        void rejectsApprovalFromNonSubmittedStatuses(ListingStatus invalidStatus) {
            // given
            listing.setStatus(invalidStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.approveListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Approved",
                    2L
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only SUBMITTED_FOR_REVIEW listings can be approved");

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when listing does not exist")
        void throwsNotFoundWhenListingMissing() {
            // given
            UUID unknownId = randomUUID();
            given(listingRepository.findById(unknownId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> adminListingService.approveListing(unknownId, ADMIN_KEYCLOAK_ID, "Reason", 1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
        }
    }


    // =========================================================================
    // 3. requestChanges
    // =========================================================================
    @Nested
    @DisplayName("requestChanges")
    class RequestChanges {

        @Test
        @DisplayName("transitions SUBMITTED_FOR_REVIEW listing to CHANGES_REQUESTED and records ListingReview and AdminAuditRecord")
        void transitionsSubmittedListingToChangesRequestedWithAudit() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));

            String reason = "Please upload clear photos of the kitchen and clarify the service charge breakdown.";

            // when
            ListingResponse response = adminListingService.requestChanges(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    reason,
                    2L
            );

            // then
            assertThat(response.status()).isEqualTo(ListingStatus.CHANGES_REQUESTED);

            then(reviewRepository).should().save(reviewCaptor.capture());
            assertThat(reviewCaptor.getValue().getDecision()).isEqualTo(CHANGES_REQUESTED);
            assertThat(reviewCaptor.getValue().getReason()).isEqualTo(reason);
            assertThat(reviewCaptor.getValue().getListingVersion()).isEqualTo(2L);

            then(auditRecordRepository).should().save(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getActionType()).isEqualTo("REQUEST_CHANGES_LISTING");
            assertThat(auditCaptor.getValue().getPreviousState()).isEqualTo("SUBMITTED_FOR_REVIEW");
            assertThat(auditCaptor.getValue().getNewState()).isEqualTo("CHANGES_REQUESTED");
        }

        @Test
        @DisplayName("rejects requesting changes on optimistic version mismatch and writes nothing")
        void rejectsRequestChangesOnVersionMismatch() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.requestChanges(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Update photos",
                    99L
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Listing has been modified since it was loaded");

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());
        }

        @ParameterizedTest(name = "rejects requesting changes when listing status is {0}")
        @EnumSource(value = ListingStatus.class, names = "SUBMITTED_FOR_REVIEW", mode = EXCLUDE)
        @DisplayName("rejects requesting changes from every status other than SUBMITTED_FOR_REVIEW and writes nothing")
        void rejectsRequestChangesFromNonSubmittedStatuses(ListingStatus invalidStatus) {
            // given
            listing.setStatus(invalidStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.requestChanges(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Update photos",
                    2L
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only SUBMITTED_FOR_REVIEW listings can have changes requested");

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());

        }
    }

    // =========================================================================
    // 4. rejectingListing
    // =========================================================================
    @Nested
    @DisplayName("rejectingListing")
    class RejectListing {
        @Test
        @DisplayName("transitions SUBMITTED_FOR_REVIEW listing to REJECTED and records ListingReview and AdminAuditRecord")
        void transitionsSubmittedListingToRejectedWithAudit() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));
            String reason = "Property ownership documents failed verification.";

            // when
            ListingResponse response = adminListingService.rejectingListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    reason,
                    2L
            );
            // then
            assertThat(response.status()).isEqualTo(REJECTED);

            then(reviewRepository).should().save(reviewCaptor.capture());
            assertThat(reviewCaptor.getValue().getDecision()).isEqualTo(ReviewDecision.REJECTED);
            assertThat(reviewCaptor.getValue().getReason()).isEqualTo(reason);
            assertThat(reviewCaptor.getValue().getListingVersion()).isEqualTo(2L);

            then(auditRecordRepository).should().save(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getActionType()).isEqualTo("REJECT_LISTING");
            assertThat(auditCaptor.getValue().getPreviousState()).isEqualTo("SUBMITTED_FOR_REVIEW");
            assertThat(auditCaptor.getValue().getNewState()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("rejects rejecting listing on optimistic version mismatch and writes nothing")
        void rejectsRejectListingOnVersionMismatch() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.rejectingListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Fraudulent listing",
                    99L
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Listing has been modified since it was loaded");

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());
        }

        @ParameterizedTest(name = "rejects rejecting listing when status is {0}")
        @EnumSource(value = ListingStatus.class, names = "SUBMITTED_FOR_REVIEW", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("rejects rejecting listing from every status other than SUBMITTED_FOR_REVIEW and writes nothing (LST-ADM-002)")
        void rejectsRejectListingFromNonSubmittedStatuses(ListingStatus invalidStatus) {
            // given
            listing.setStatus(invalidStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.rejectingListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Fraudulent listing",
                    2L
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only SUBMITTED_FOR_REVIEW listings can be rejected");

            then(reviewRepository).shouldHaveNoInteractions();
            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());
        }

    }

    // =========================================================================
    // 5. suspendListing
    // =========================================================================
    @Nested
    @DisplayName("suspendListing")
    class SuspendListing {
        void suspendsPublishedListingWithAuditRecord() {
            // given
            listing.setStatus(ListingStatus.PUBLISHED);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));
            String reason = "Multiple tenant fraud reports under investigation.";

            // when
            ListingResponse response = adminListingService.suspendListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    reason
            );

            // then
            assertThat(response.status()).isEqualTo(ListingStatus.SUSPENDED);

            then(auditRecordRepository).should().save(auditCaptor.capture());
            AdminAuditRecord audit = auditCaptor.getValue();
            assertThat(audit.getAdminKeycloakId()).isEqualTo(ADMIN_KEYCLOAK_ID);
            assertThat(audit.getActionType()).isEqualTo("SUSPEND_LISTING");
            assertThat(audit.getEntityType()).isEqualTo("Listing");
            assertThat(audit.getEntityId()).isEqualTo(listingId.toString());
            assertThat(audit.getReason()).isEqualTo(reason);
            assertThat(audit.getPreviousState()).isEqualTo("PUBLISHED");
            assertThat(audit.getNewState()).isEqualTo("SUSPENDED");
        }

        @ParameterizedTest(name = "rejects suspending listing when status is {0}")
        @EnumSource(value = ListingStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("rejects suspending listing from every status other than PUBLISHED and writes nothing (LST-ADM-003)")
        void rejectsSuspendingFromNonPublishedStatuses(ListingStatus nonPublishedStatus) {
            // given
            listing.setStatus(nonPublishedStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> adminListingService.suspendListing(
                    listingId,
                    ADMIN_KEYCLOAK_ID,
                    "Suspending non-published listing"
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only PUBLISHED listings can be suspended");

            then(auditRecordRepository).shouldHaveNoInteractions();
            then(listingRepository).should(never()).save(any());
        }


        @Test
        @DisplayName("throws ResourceNotFoundException when listing does not exist")
        void throwsNotFoundWhenListingMissing() {
            // given
            UUID unknownId = UUID.randomUUID();
            given(listingRepository.findById(unknownId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> adminListingService.suspendListing(unknownId, ADMIN_KEYCLOAK_ID, "Reason"))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(auditRecordRepository).shouldHaveNoInteractions();
        }
        
    }
}
