package com.lagosrent.listing.application.service;

import com.lagosrent.listing.api.dto.*;
import com.lagosrent.listing.application.exception.ResourceNotFoundException;
import com.lagosrent.listing.domain.model.*;
import com.lagosrent.listing.infrastructure.messaging.payload.ListingPublishedPayload;
import com.lagosrent.listing.infrastructure.persistence.ListingMediaRepository;
import com.lagosrent.listing.infrastructure.persistence.ListingRepository;
import com.lagosrent.listing.infrastructure.persistence.RentalUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lagosrent.listing.domain.model.FeeFrequency.ANNUALLY;
import static com.lagosrent.listing.domain.model.FeeFrequency.ONE_OFF;
import static com.lagosrent.listing.domain.model.FeeRecipient.*;
import static com.lagosrent.listing.domain.model.FeeType.*;
import static com.lagosrent.listing.domain.model.ListingStatus.*;
import static com.lagosrent.listing.domain.model.MediaType.IMAGE;
import static java.math.BigDecimal.ZERO;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("Layer 1: ListingService State Machine & Invariant Unit Tests")
public class ListingServiceTest {

    // Verified seeded landlord identity
    private static final String OWNER_LANDLORD_ID = "818746e8-5729-4aa5-91fc-a99199dbd2aa";
    private static final String OTHER_LANDLORD_ID = "99999999-0000-4aa5-91fc-ffffffffffff";

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private RentalUnitRepository rentalUnitRepository;
    @Mock
    private ListingMediaRepository listingMediaRepository;
    @Mock
    private OutboxService outboxService;

    @Captor
    private ArgumentCaptor<Listing> listingCaptor;

    @Captor
    private ArgumentCaptor<ListingMedia> mediaCaptor;

    @Captor
    private ArgumentCaptor<ListingPublishedPayload> publishedPayloadCaptor;

    private ListingService listingService;

    private UUID propertyId;
    private UUID unitId;
    private UUID listingId;
    private Property property;
    private RentalUnit unit;
    private Listing listing;


    @BeforeEach
    void setUp() {
        listingService = new ListingService(
                listingRepository,
                rentalUnitRepository,
                listingMediaRepository,
                outboxService
        );

        propertyId = randomUUID();
        unitId = randomUUID();
        listingId = randomUUID();

        property = new Property(
                OWNER_LANDLORD_ID,
                "Admiralty Heights",
                "Eti-Osa",
                "Lekki Phase 1",
                "Plot 14 Admiralty Way",
                "Beside Daytona Supermarket"
        );
        property.setLatitude(new BigDecimal("6.4474000"));
        property.setLongitude(new BigDecimal("3.4723000"));
        ReflectionTestUtils.setField(property, "id", propertyId);

        unit = new RentalUnit(property, "Flat 2B", UnitType.APARTMENT, 3, 3, 4);
        unit.setSizeSqMeters(new BigDecimal("140.00"));
        unit.setFloorNumber(2);
        ReflectionTestUtils.setField(unit, "id", unitId);

        listing = new Listing(
                unit,
                OWNER_LANDLORD_ID,
                "Serviced 3-Bedroom Apartment in Lekki Phase 1",
                "24/7 power and treated water",
                new BigDecimal("4500000.00"),
                LocalDate.of(2026, 12, 1)
        );
        ReflectionTestUtils.setField(listing, "id", listingId);
        ReflectionTestUtils.setField(listing, "version", 1L);

    }

    // =========================================================================
    // 1. createListing
    // =========================================================================
    @Nested
    @DisplayName("createListing")
    class CreateListing {
        @Test
        @DisplayName("creates a DRAFT listing, sums base rent + ONE_OFF fees into moveInTotal (excluding recurring fees), and sets fee approval flags")
        void createsDraftListingAndCalculatesMoveInTotalFromOneOffFeesOnly() {
            // given: base rent 4,500,000 + ONE_OFF caution 450,000 + ONE_OFF other 50,000 = 5,000,000
            //        (ANNUALLY service charge 600,000 is recurring and excluded from moveInTotal)
            List<ListingFeeDto> feeDtos = List.of(
                    new ListingFeeDto(
                            null,
                            CAUTION_DEPOSIT,
                            new BigDecimal("450000.00"),
                            LANDLORD,
                            ONE_OFF,
                            true,
                            "Refundable caution deposit",
                            null
                    ),
                    new ListingFeeDto(
                            null,
                            SERVICE_CHARGE,
                            new BigDecimal("600000.00"),
                            PROPERTY_MANAGER,
                            ANNUALLY,
                            false,
                            "Annual diesel and estate security",
                            null
                    ),
                    new ListingFeeDto(
                            null,
                            OTHER,
                            new BigDecimal("50000.00"),
                            PROPERTY_MANAGER,
                            ONE_OFF,
                            false,
                            "Biometric access card",
                            null
                    )
            );

            CreateListingRequest request = new CreateListingRequest(
                    "Serviced 3-Bedroom Apartment",
                    "Modern finishes",
                    new BigDecimal("4500000.00"),
                    "ANNUALLY",
                    LocalDate.of(2026, 12, 1),
                    feeDtos
            );

            given(rentalUnitRepository.findById(unitId)).willReturn(Optional.of(unit));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> {
                Listing saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", listingId);
                return saved;
            });

            // when
            ListingResponse response = listingService.createListing(unitId, OWNER_LANDLORD_ID, request);

            // then
            then(listingRepository).should().save(listingCaptor.capture());
            Listing captured = listingCaptor.getValue();

            assertThat(captured.getStatus()).isEqualTo(DRAFT);
            assertThat(captured.getRentAmount()).isEqualByComparingTo("4500000.00");
            assertThat(captured.getMoveInTotal()).isEqualByComparingTo("5000000.00");
            assertThat(captured.getFees()).hasSize(3);

            assertThat(captured.getFees())
                    .filteredOn(f -> f.getFeeType() != OTHER)
                    .allMatch(ListingFee::isApprovedByAdmin);
            assertThat(captured.getFees())
                    .filteredOn(f -> f.getFeeType() == OTHER)
                    .allMatch(f -> !f.isApprovedByAdmin());

            assertThat(response.id()).isEqualTo(listingId);
            assertThat(response.status()).isEqualTo(DRAFT);
            assertThat(response.moveInTotal()).isEqualByComparingTo("5000000.00");
            assertThat(response.fees()).hasSize(3);
        }

        @Test
        @DisplayName("defaults rentPeriod to ANNUALLY and moveInTotal to rentAmount when optional fields are null")
        void defaultsRentPeriodAndMoveInTotalWhenOptionalFieldsNull() {
            // given
            CreateListingRequest request = new CreateListingRequest(
                    "Studio in Yaba",
                    "Close to tech hub",
                    new BigDecimal("1200000.00"),
                    null,
                    LocalDate.of(2026, 12, 15),
                    null
            );

            given(rentalUnitRepository.findById(unitId)).willReturn(Optional.of(unit));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ListingResponse response = listingService.createListing(unitId, OWNER_LANDLORD_ID, request);

            // then
            assertThat(response.rentPeriod()).isEqualTo("ANNUALLY");
            assertThat(response.moveInTotal()).isEqualByComparingTo("1200000.00");
            assertThat(response.fees()).isEmpty();

        }

        @Test
        @DisplayName("throws ResourceNotFoundException and saves nothing RentalUnit does not exist")
        void throwsNotFoundWhenRentalUnitMissing() {
            // given
            UUID unknownUnitId = randomUUID();
            given(rentalUnitRepository.findById(unknownUnitId)).willReturn(Optional.empty());
            CreateListingRequest request = new CreateListingRequest("Title", "Desc", new BigDecimal("2000000.00"), "ANNUALLY", LocalDate.now(), List.of());

            // when / then
            assertThatThrownBy(() -> listingService.createListing(unknownUnitId, OWNER_LANDLORD_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownUnitId.toString());

            then(listingRepository).shouldHaveNoInteractions();
        }


        @Test
        @DisplayName("rejects with IllegalArgumentException and saves nothing when caller does not own the unit's Property")
        void rejectsWhenCallerDoesNotOwnProperty() {
            // given
            given(rentalUnitRepository.findById(unitId)).willReturn(Optional.of(unit));

            CreateListingRequest request = new CreateListingRequest(
                    "Title", "Desc", new BigDecimal("2000000.00"), "ANNUALLY", LocalDate.now(), List.of()
            );

            // when / then
            assertThatThrownBy(() -> listingService.createListing(unitId, OTHER_LANDLORD_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You do not own the property for this rental unit");

            then(listingRepository).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // 2. updateListing
    // =========================================================================
    @Nested
    @DisplayName("updateListing")
    class UpdateListing {
        private final UpdateListingRequest validUpdateRequest = new UpdateListingRequest(
                "Updated 3-Bedroom Apartment",
                "Newly painted with solar inverter",
                new BigDecimal("4800000.00"),
                "ANNUALLY",
                LocalDate.of(2027, 1, 1),
                List.of(
                        new ListingFeeDto(
                                null,
                                CAUTION_DEPOSIT,
                                new BigDecimal("500000.00"),
                                LANDLORD,
                                ONE_OFF,
                                true,
                                "Updated caution deposit",
                                null
                        )
                )
        );

        @ParameterizedTest(name = "updates listing and recalculates moveInTotal when status is {0}")
        @EnumSource(value = ListingStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"})
        @DisplayName("updates fields, replaces fee lines, and recalculates moveInTotal for DRAFT or CHANGES_REQUESTED listings")
        void updatesEditableListingAndRecalculatesTotal(ListingStatus editableStatus) {
            // given
            listing.setStatus(editableStatus);
            listing.getFees().add(new ListingFee(
                    listing, AGENCY, new BigDecimal("100000.00"),
                    AGENT, ONE_OFF, false, "Old fee to be replaced"
            ));

            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ListingResponse response = listingService.updateListing(listingId, OWNER_LANDLORD_ID, validUpdateRequest);

            // then: 4,800,000 rent + 500,000 new caution deposit = 5,300,000 (old 100,000 fee cleared)
            assertThat(response.title()).isEqualTo("Updated 3-Bedroom Apartment");
            assertThat(response.rentAmount()).isEqualByComparingTo("4800000.00");
            assertThat(response.moveInTotal()).isEqualByComparingTo("5300000.00");
            assertThat(response.fees()).hasSize(1);
            assertThat(response.fees().getFirst().feeType()).isEqualTo(CAUTION_DEPOSIT);
        }

        @ParameterizedTest(name = "rejects editing when listing is in status {0}")
        @EnumSource(value = ListingStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"}, mode = EXCLUDE)
        @DisplayName("rejects editing from every status other than DRAFT and CHANGES_REQUESTED")
        void rejectsEditingFromNonEditableStatuses(ListingStatus nonEditableStatus) {
            // given
            listing.setStatus(nonEditableStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.updateListing(listingId, OWNER_LANDLORD_ID, validUpdateRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only DRAFT or CHANGES_REQUESTED listings can be edited");

            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("rejects with IllegalArgumentException and saves nothing when caller is not the listing owner")
        void rejectsWhenCallerDoesNotOwnListing() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.updateListing(listingId, OTHER_LANDLORD_ID, validUpdateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You do not own this listing");

            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when listing does not exist")
        void throwsNotFoundWhenListingMissing() {
            // given
            UUID unknownId = randomUUID();
            given(listingRepository.findById(unknownId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> listingService.updateListing(unknownId, OWNER_LANDLORD_ID, validUpdateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // 3. submitForReview
    // =========================================================================
    @Nested
    @DisplayName("submitForReview")
    class SubmitForReview {
        @ParameterizedTest(name = "transitions {0} listing to SUBMITTED_FOR_REVIEW and recalculates moveInTotal")
        @EnumSource(value = ListingStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"})
        @DisplayName("transitions DRAFT or CHANGES_REQUESTED listing to SUBMITTED_FOR_REVIEW")
        void transitionsSubmittableListingToSubmittedForReview(ListingStatus submittableStatus) {
            // given
            listing.setStatus(submittableStatus);
            listing.getFees().add(new ListingFee(
                    listing, CAUTION_DEPOSIT, new BigDecimal("450000.00"),
                    LANDLORD, ONE_OFF, true, "Refundable caution"
            ));

            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.save(any(Listing.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ListingResponse response = listingService.submitForReview(listingId, OWNER_LANDLORD_ID);

            // then
            assertThat(response.status()).isEqualTo(SUBMITTED_FOR_REVIEW);
            assertThat(response.moveInTotal()).isEqualByComparingTo("4950000.00");
            then(listingRepository).should().save(listing);
        }

        @ParameterizedTest(name = "rejects submission for status {0}")
        @EnumSource(value = ListingStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"}, mode = EXCLUDE)
        @DisplayName("rejects submission from every status other than DRAFT and CHANGES_REQUESTED")
        void rejectsSubmissionFromNonSubmittableStatus(ListingStatus nonSubmittableStatus) {
            // given
            listing.setStatus(nonSubmittableStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.submitForReview(listingId, OWNER_LANDLORD_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Listing is not in a submittable state");

            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("rejects submission when rentAmount is zero, negative, or null")
        void rejectsSubmissionWhenRentAmountIsNotPositive() {
            // given
            listing.setRentAmount(ZERO);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.submitForReview(listingId, OWNER_LANDLORD_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Rent amount is required to submit listing");

            then(listingRepository).should(never()).save(any());

        }

        @Test
        @DisplayName("rejects submission when caller does not own the listing")
        void rejectsSubmissionWhenCallerIsNotOwner() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.submitForReview(listingId, OTHER_LANDLORD_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You do not own this listing");

            then(listingRepository).should(never()).save(any());
        }
    }

    // =========================================================================
    // 4. publishListing
    // =========================================================================

    @Nested
    @DisplayName("publishListing")
    class PublishListing {

        @Test
        @DisplayName("transitions APPROVED listing to PUBLISHED, flushes version BEFORE recording listing.published.v1 outbox event, and does not double-save" +
                " (LST-SVC-001)")
        void publishesApprovedListingAndRecordsOutboxEventInOrder() {
            // given
            listing.setStatus(APPROVED);
            listing.getFees().add(new ListingFee(
                    listing, CAUTION_DEPOSIT, new BigDecimal("450000.00"),
                    LANDLORD, ONE_OFF, true, "Refundable caution"
            ));
            listing.setMoveInTotal(new BigDecimal("4950000.00"));
            listing.getMedia().add(new ListingMedia(
                    listing, IMAGE, "listings/" + listingId + "/cover.jpg", "cover.jpg", 0, true

            ));

            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingRepository.saveAndFlush(any(Listing.class))).willAnswer(invocation -> {
                Listing flushed = invocation.getArgument(0);
                ReflectionTestUtils.setField(flushed, "version", 2L);
                return flushed;
            });

            // when
            ListingResponse response = listingService.publishListing(listingId, OWNER_LANDLORD_ID);

            // then: 1. saveAndFlush happens BEFORE outboxService.recordEvent, and save() is not called a second time
            InOrder inOrder = inOrder(listingRepository, outboxService);
            inOrder.verify(listingRepository).saveAndFlush(listing);
            inOrder.verify(outboxService).recordEvent(
                    eq("listing"),
                    eq(listingId.toString()),
                    eq(2L),
                    eq("listing.published.v1"),
                    publishedPayloadCaptor.capture(),
                    isNull(),
                    isNull()
            );
            then(listingRepository).should(never()).save(any());

            // 2. Verify returned response and outbox projection payload
            assertThat(response.status()).isEqualTo(PUBLISHED);
            assertThat(response.version()).isEqualTo(2L);

            ListingPublishedPayload payload = publishedPayloadCaptor.getValue();
            assertThat(payload.listingId()).isEqualTo(listingId);
            assertThat(payload.landlordKeycloakId()).isEqualTo(OWNER_LANDLORD_ID);
            assertThat(payload.propertyId()).isEqualTo(propertyId);
            assertThat(payload.rentalUnitId()).isEqualTo(unitId);
            assertThat(payload.rentAmount()).isEqualByComparingTo("4500000.00");
            assertThat(payload.moveInTotal()).isEqualByComparingTo("4950000.00");
            assertThat(payload.state()).isEqualTo("Lagos");
            assertThat(payload.lga()).isEqualTo("Eti-Osa");
            assertThat(payload.area()).isEqualTo("Lekki Phase 1");
            assertThat(payload.landmark()).isEqualTo("Beside Daytona Supermarket");
            assertThat(payload.unitType()).isEqualTo("APARTMENT");
            assertThat(payload.bedrooms()).isEqualTo(3);
            assertThat(payload.fees()).hasSize(1);
            assertThat(payload.media()).hasSize(1);
        }

        @ParameterizedTest(name = "rejects publishing when listing status is {0}")
        @EnumSource(value = ListingStatus.class, names = "APPROVED", mode = EXCLUDE)
        @DisplayName("rejects publishing from every status other than APPROVED and records zero outbox events")
        void rejectsPublishingFromNonApprovedStatuses(ListingStatus nonApprovedStatus) {
            // given
            listing.setStatus(nonApprovedStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.publishListing(listingId, OWNER_LANDLORD_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only APPROVED listings can be published");

            then(listingRepository).should(never()).saveAndFlush(any());
            then(outboxService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("rejects publishing when caller does not own the listing and records zero outbox events")
        void rejectsPublishingWhenCallerIsNotOwner() {
            // given
            listing.setStatus(APPROVED);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.publishListing(listingId, OTHER_LANDLORD_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You do not own this listing");

            then(listingRepository).should(never()).saveAndFlush(any());
            then(outboxService).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // 5. addMedia
    // =========================================================================
    @Nested
    @DisplayName("addMedia")
    class AddMedia {
        private final ListingMediaDto validMediaDto = new ListingMediaDto(
                null,
                IMAGE,
                "listings/front-elevation.jpg",
                "front-elevation.jpg",
                null,
                "Front elevation view",
                null
        );

        @ParameterizedTest(name = "attaches media with safe defaults when listing status is {0}")
        @EnumSource(value = ListingStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"})
        @DisplayName("attaches media and defaults orderIndex to 0 and isPrimary to false for editable listings")
        void attachesMediaForEditableListing(ListingStatus editableStatus) {
            // given
            listing.setStatus(editableStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));
            given(listingMediaRepository.save(any(ListingMedia.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ListingMediaDto savedDto = listingService.addMedia(listingId, OWNER_LANDLORD_ID, validMediaDto);

            // then
            then(listingMediaRepository).should().save(mediaCaptor.capture());
            ListingMedia captured = mediaCaptor.getValue();

            assertThat(captured.getListing()).isSameAs(listing);
            assertThat(captured.getS3Key()).isEqualTo("listings/front-elevation.jpg");
            assertThat(captured.getOrderIndex()).isZero();
            assertThat(captured.isPrimary()).isFalse();
            assertThat(savedDto.caption()).isEqualTo("Front elevation view");
        }

        @ParameterizedTest(name = "rejects adding unreviewed media when listing status is {0}")
        @EnumSource(value = ListingStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"}, mode = EXCLUDE)
        @DisplayName("rejects adding media when listing is not in DRAFT or CHANGES_REQUESTED")
        void rejectsAddingMediaWhenListingNotEditable(ListingStatus nonEditableStatus) {
            // given
            listing.setStatus(nonEditableStatus);
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.addMedia(listingId, OWNER_LANDLORD_ID, validMediaDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only DRAFT or CHANGES_REQUESTED listings can be modified");

            then(listingMediaRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("rejects adding media when caller does not own the listing")
        void rejectsAddingMediaWhenCallerIsNotOwner() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when / then
            assertThatThrownBy(() -> listingService.addMedia(listingId, OTHER_LANDLORD_ID, validMediaDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You do not own this listing");

            then(listingMediaRepository).shouldHaveNoInteractions();
        }
    }


    // =========================================================================
    // 6. getListingById & getLandlordListings
    // =========================================================================
    @Nested
    @DisplayName("getListingById & getLandlordListings")
    class ReadListing {
        @Test
        @DisplayName("getListingById returns mapped ListingResponse when listing exists")
        void returnsListingByIdWhenFound() {
            // given
            given(listingRepository.findById(listingId)).willReturn(Optional.of(listing));

            // when
            ListingResponse response = listingService.getListingById(listingId);

            // then
            assertThat(response.id()).isEqualTo(listingId);
            assertThat(response.rentalUnitId()).isEqualTo(unitId);
            assertThat(response.landlordKeycloakId()).isEqualTo(OWNER_LANDLORD_ID);
        }

        @Test
        @DisplayName("getListingById throws ResourceNotFoundException when listing does not exist")
        void throwsNotFoundWhenListingMissing() {
            // given
            UUID unknownId = randomUUID();
            given(listingRepository.findById(unknownId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> listingService.getListingById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        @DisplayName("getLandlordListings returns all listings owned by the landlord")
        void returnsLandlordListings() {
            // given
            given(listingRepository.findByLandlordKeycloakId(OWNER_LANDLORD_ID)).willReturn(List.of(listing));

            // when
            List<ListingResponse> results = listingService.getLandlordListings(OWNER_LANDLORD_ID);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(listingId);
        }
    }
}
