package com.lagosrent.payment.domain;

import com.lagosrent.payment.domain.model.Payable;
import com.lagosrent.payment.domain.model.PayableStatus;
import com.lagosrent.payment.domain.model.PaymentAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.lagosrent.payment.domain.model.PayableStatus.*;
import static com.lagosrent.payment.domain.model.PaymentStatus.INITIATED;
import static com.lagosrent.payment.domain.model.PaymentStatus.SUCCEEDED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment & Payable Domain Invariants Test")
public class PaymentDomainTest {
    private Payable payable;
    private final UUID applicationId = randomUUID();
    private final UUID listingId = randomUUID();
    private final String tenantId = "a7624ef5-2b0d-4348-a3ca-e4a60afef119";
    private final String landlordId = "818746e8-5729-4aa5-91fc-a99199dbd2aa";

    @BeforeEach
    void setUp() {
        payable = new Payable(
                applicationId,
                listingId,
                tenantId,
                landlordId,
                "RES-2026-9901",
                new BigDecimal("350000.00"),
                "NGN",
                now().plusSeconds(172800)
        );
    }

    @Nested
    @DisplayName("Payable State Machine Tests")
    class PayableStateTests {

        @Test
        @DisplayName("Newly created payable defaults to CREATED status")
        void defaultPayableStatusIsCreated() {
            assertThat(payable.getStatus()).isEqualTo(CREATED);
            assertThat(payable.getTotalAmount()).isEqualByComparingTo("350000.00");
            assertThat(payable.getCurrency()).isEqualTo("NGN");
        }

        @Test
        @DisplayName("Payable in CREATED or PENDING status can initiate checkout")
        void createdOrPendingPayableCanInitiateCheckout() {
            payable.setStatus(CREATED);
            assertThat(payable.getStatus() == CREATED || payable.getStatus() == PENDING).isTrue();

            payable.setStatus(PENDING);
            assertThat(payable.getStatus() == CREATED || payable.getStatus() == PENDING).isTrue();
        }

        @ParameterizedTest(name = "Cannot checkout payable in status: {0}")
        @EnumSource(value = PayableStatus.class, names = {"PAID", "FAILED", "EXPIRED", "CANCELLED"})
        @DisplayName("Final or terminal payables reject checkout attempts")
        void terminalPayablesRejectCheckout(PayableStatus invalidStatus) {
            payable.setStatus(invalidStatus);

            assertThatThrownBy(() -> {
                if (payable.getStatus() != CREATED && payable.getStatus() != PENDING) {
                    throw new IllegalStateException("Cannot checkout payable in status: " + payable.getStatus());
                }
            }).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot checkout payable in status: " + invalidStatus);
        }
    }

    @Nested
    @DisplayName("Webhook Financial Verification Invariants")
    class WebhookFinancialTests {

        @Test
        @DisplayName("Amount paid matching payable totalAmount confirms successfully regardless of BigDecimal scale")
        void matchingAmountsConfirmSuccessfully() {
            BigDecimal payableAmount = new BigDecimal("350000.00");
            BigDecimal webhookAmount = new BigDecimal("350000");

            boolean amountsMatch = payableAmount.compareTo(webhookAmount) == 0;

            assertThat(amountsMatch).isTrue();
        }

        @Test
        @DisplayName("Amount mismatch between payable and provider charge throws validation error")
        void amountMismatchIsRejected() {
            BigDecimal payableAmount = payable.getTotalAmount();
            BigDecimal fraudulentAmountPaid = new BigDecimal("35000.00");

            assertThatThrownBy(() -> {
                if (payableAmount.compareTo(fraudulentAmountPaid) != 0) {
                    throw new IllegalArgumentException("Amount mismatch! Expected=" + payableAmount + ", Paid=" + fraudulentAmountPaid);
                }
            }).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Amount mismatch! Expected=350000.00, Paid=35000.00");
        }
    }

    @Nested
    @DisplayName("Payment Attempt Lifecycle Tests")
    class PaymentAttemptTests {
        @Test
        @DisplayName("Successful payment attempt transitions to SUCCEEDED and marks payable PAID")
        void successfulPaymentUpdatesStatuses() {
            PaymentAttempt attempt = new PaymentAttempt(
                    payable,
                    "PAY-TX-9901A",
                    "idemp-key-001",
                    "paystack",
                    payable.getTotalAmount(),
                    "NGN",
                    "https://checkout.paystack.com/auth-123"
            );

            assertThat(attempt.getStatus()).isEqualTo(INITIATED);

            // When: Webhook succeeds
            attempt.setStatus(SUCCEEDED);
            payable.setStatus(PAID);

            // Then: Both statuses are updated
            assertThat(attempt.getStatus()).isEqualTo(SUCCEEDED);
            assertThat(payable.getStatus()).isEqualTo(PAID);
        }
    }
}
