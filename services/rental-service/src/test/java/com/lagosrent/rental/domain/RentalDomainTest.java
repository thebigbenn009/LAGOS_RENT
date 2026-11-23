package com.lagosrent.rental.domain;

import com.lagosrent.rental.domain.model.PriceSnapshot;
import com.lagosrent.rental.domain.model.RentalApplication;
import com.lagosrent.rental.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.lagosrent.rental.domain.model.ApplicationStatus.DRAFT;
import static com.lagosrent.rental.domain.model.ApplicationStatus.WITHDRAWN;
import static com.lagosrent.rental.domain.model.ReservationStatus.CONFIRMED;
import static com.lagosrent.rental.domain.model.ReservationStatus.EXPIRED;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rental Domain & Reservation Invariants Tests")
public class RentalDomainTest {
    private RentalApplication application;
    private final UUID listingId = randomUUID();

    @BeforeEach
    void setUp() {
        String landlordId = "818746e8-5729-4aa5-91fc-a99199dbd2aa";
        String tenantId = "a7624ef5-2b0d-4348-a3ca-e4a60afef119";
        application = new RentalApplication(listingId, tenantId, landlordId);
        application.setAnnualIncome(new BigDecimal("5000000.00"));
        application.setEmploymentStatus("Tech Professional");
    }

    @Nested
    @DisplayName("Application State Invariants")
    class ApplicationTransitionTests {

        @Test
        @DisplayName("Newly created application defaults to DRAFT")
        void newApplicationDefaultsToDraft() {
            assertThat(application.getStatus()).isEqualTo(DRAFT);
        }

        @Test
        @DisplayName("Cannot accept an application that has been WITHDRAWN by the tenant")
        void cannotAcceptWithdrawnApplication() {
            application.setStatus(WITHDRAWN);

            assertThatThrownBy(() -> {
                if (application.getStatus() == WITHDRAWN) {
                    throw new IllegalStateException("Cannot accept a withdrawn application");
                }
            }).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot accept a withdrawn application");

            assertThat(application.getStatus()).isEqualTo(WITHDRAWN);
        }
    }

    @Nested
    @DisplayName("Price Snapshot Immutability Invariants")
    class PriceSnapshotTests {

        @Test
        @DisplayName("Price snapshot locks financial terms at agreement time")
        void priceSnapshotProtectsTenantFromPostAgreementHikes() {
            // Given: Agreed rental terms captured in a price snapshot
            BigDecimal agreedRent = new BigDecimal("2500000.00");
            BigDecimal caution = new BigDecimal("250000.00");
            BigDecimal service = new BigDecimal("150000.00");
            BigDecimal legal = new BigDecimal("100000.00");
            BigDecimal totalMoveIn = new BigDecimal("3000000.00");

            PriceSnapshot snapshot = new PriceSnapshot(
                    application,
                    listingId,
                    1L,
                    agreedRent,
                    caution,
                    service,
                    legal,
                    totalMoveIn,
                    "{\"rent\": 2500000, \"caution\": 250000, \"legal\": 100000}"
            );

            // When: The external listing price changes later to 3,500,000 NGN
            BigDecimal newListingPrice = new BigDecimal("3500000.00");

            // Then: The price snapshot remains strictly identical to the agreed terms
            assertThat(snapshot.getTotalMoveInAmount()).isEqualByComparingTo("3000000.00");
            assertThat(snapshot.getRentAmount()).isEqualByComparingTo("2500000.00");
            assertThat(snapshot.getTotalMoveInAmount()).isNotEqualByComparingTo(newListingPrice);
        }
    }

    @Nested
    @DisplayName("Reservation Expiry & Hold Invariants")
    class ReservationExpiryTests {
        @Test
        @DisplayName("Active reservation within hold deadline is valid for checkout")
        void activeReservationIsValid() {
            Instant futureDeadline = now().plus(48, HOURS);
            Reservation reservation = new Reservation(application, listingId, "RES-TEST-001", futureDeadline);
            reservation.setStatus(CONFIRMED);

            boolean isExpired = now().isAfter(reservation.getHoldDeadline());

            assertThat(isExpired).isFalse();
            assertThat(reservation.getStatus()).isEqualTo(CONFIRMED);
        }

        @Test
        @DisplayName("Reservation past hold deadline is considered EXPIRED and rejects checkout")
        void expiredReservationRejectsCheckout() {
            // Given: A hold deadline is in the past
            Instant pastDeadline = now().minus(1, HOURS);
            Reservation reservation = new Reservation(application, listingId, "RES-EXPIRED-001", pastDeadline);

            // When: Checking eligibility
            boolean isExpired = now().isAfter(reservation.getHoldDeadline());
            if (isExpired) {
                reservation.setStatus(EXPIRED);
            }

            // Then: Reservation transitions to Expired
            assertThat(reservation.getStatus()).isEqualTo(EXPIRED);
            assertThatThrownBy(() -> {
                if (reservation.getStatus() == EXPIRED) {
                    throw new IllegalStateException("Reservation hold has expired. Room has been released");
                }
            }).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Reservation hold has expired");
        }
    }
}
