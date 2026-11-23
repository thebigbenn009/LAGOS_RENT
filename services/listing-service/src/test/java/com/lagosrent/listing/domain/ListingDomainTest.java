package com.lagosrent.listing.domain;

import com.lagosrent.listing.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.lagosrent.listing.domain.model.FeeFrequency.ONE_OFF;
import static com.lagosrent.listing.domain.model.FeeRecipient.LANDLORD;
import static com.lagosrent.listing.domain.model.FeeRecipient.PROPERTY_MANAGER;
import static com.lagosrent.listing.domain.model.FeeType.CAUTION_DEPOSIT;
import static com.lagosrent.listing.domain.model.FeeType.OTHER;
import static com.lagosrent.listing.domain.model.UnitType.APARTMENT;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Listing Domain Invariants & State Machine Tests")
public class ListingDomainTest {
    private static final String LANDLORD_KEYCLOAK_ID = "818746e8-5729-4aa5-91fc-a99199dbd2aa";
    private static final String ADMIN_KEYCLOAK_ID = "admin_test";

    private Property property;
    private RentalUnit rentalUnit;
    private Listing listing;

    @BeforeEach
    void setUp() {
        property = new Property(
                LANDLORD_KEYCLOAK_ID,
                "Admiralty Heights",
                "Eti-Osa",
                "Lekki Phase 1",
                "Plot 14 Admiralty Way",
                "Beside Daytona Supermarket"
        );

        rentalUnit = new RentalUnit(
                property,
                "Flat 2B",
                APARTMENT,
                3,
                3,
                4
        );

        listing = new Listing(
                rentalUnit,
                LANDLORD_KEYCLOAK_ID,
                "Serviced 3-Bedroom Apartment in Lekki Phase 1",
                "24-hour power, treated water, and dedicated parking.",
                new BigDecimal("4500000.00"),
                LocalDate.of(2026, 12, 1)
        );
    }

    @Nested
    @DisplayName("ListingFee transparency & admin-approval invariants")
    class ListingFeeTransparencyInvariants {

        @ParameterizedTest(name = "Standard FeeType.{0} is auto-approved (approvedByAdmin = true)")
        @EnumSource(value = FeeType.class, names = "OTHER", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("auto-approves every standard (non-OTHER) fee type on creation")
        void autoApprovesStandardFeeTypes(FeeType standardFeeType) {
            boolean refundable = (standardFeeType == CAUTION_DEPOSIT);
            ListingFee fee = new ListingFee(
                    listing,
                    standardFeeType,
                    new BigDecimal("450000.00"),
                    LANDLORD,
                    ONE_OFF,
                    refundable,
                    "Transparent breakdown for " + standardFeeType
            );

            assertThat(fee.isApprovedByAdmin()).isTrue();

        }

        @Test
        @DisplayName("marks FeeType.OTHER as unapproved until an admin explicitly approves the listing")
        void marksOtherFeeAsUnapprovedByDefault() {
            ListingFee otherFee = new ListingFee(
                    listing,
                    OTHER,
                    new BigDecimal("75000.00"),
                    PROPERTY_MANAGER,
                    ONE_OFF,
                    false,
                    "Estate access card and biometric registration"
            );

            assertThat(otherFee.isApprovedByAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("Listing initial state & lifecycle invariants")
    class ListingInitialState {

        @Test
        @DisplayName("new Listing starts in DRAFT status, ANNUALLY rentPeriod, and moveInTotal equal to base rentAmount")
        void newListingStartsInDraftWithMoveInTotalEqualToRent() {
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.DRAFT);
            assertThat(listing.getRentPeriod()).isEqualTo("ANNUALLY");
            assertThat(listing.getMoveInTotal()).isEqualByComparingTo("4500000.00");
            assertThat(listing.getFees()).isEmpty();
            assertThat(listing.getMedia()).isEmpty();
        }

        @Test
        @DisplayName("onUpdate advances updatedAt without modifying createdAt (LST-DOM-001)")
        void onUpdateAdvancesUpdatedAtOnly() {
            Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
            listing.setCreatedAt(createdAt);
            listing.setUpdatedAt(createdAt);

            listing.onUpdate();

            assertThat(listing.getCreatedAt()).isEqualTo(createdAt);
            assertThat(listing.getUpdatedAt()).isAfter(createdAt);
        }
    }

    @Nested
    @DisplayName("Property & ListingReservation initial state and invariants")
    class PropertyAndReservationInitialState {

        @Test
        @DisplayName("new Property defaults state to 'Lagos'")
        void newPropertyDefaultsToLagos() {
            assertThat(property.getState()).isEqualTo("Lagos");
        }


        @Test
        @DisplayName("new ListingReservation starts in ACTIVE status")
        void newReservationStartsInActiveStatus() {
            ListingReservation reservation = new ListingReservation(
                    listing,
                    "RES-2026-0001",
                    Instant.now().plus(24, ChronoUnit.HOURS)
            );

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        }

        @Nested
        @DisplayName("OutboxEvent initial state invariants")
        class OutboxEventInitialState {

            @Test
            @DisplayName("new OutboxEvent starts unpublished (publishedAt = null, attemptCount = 0) and generates UUID when eventId is null")
            void newOutboxEventStartsUnpublishedWithFallbackId() {
                OutboxEvent event = new OutboxEvent(
                        null,
                        "listing",
                        UUID.randomUUID().toString(),
                        1L,
                        "listing.published.v1",
                        "{}",
                        null,
                        null
                );

                assertThat(event.getId()).isNotNull();
                assertThat(event.getAttemptCount()).isZero();
                assertThat(event.getPublishedAt()).isNull();
            }
        }

    }
}

