package com.lagosrent.payment.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;
import static java.time.Instant.now;


@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private PaymentAttempt attempt;

    @Column(name = "transaction_reference", nullable = false, unique = true)
    private String transactionReference;

    @Column(name = "provider_reference", nullable = false, unique = true)
    private String providerReference;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "NGN";

    private String channel;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = now();

    public PaymentTransaction(Payable payable, PaymentAttempt attempt,
                              String transactionReference,
                              String providerReference, String providerName, BigDecimal amount,
                              String currency, String channel, Instant paidAt) {
        this.payable = payable;
        this.attempt = attempt;
        this.transactionReference = transactionReference;
        this.providerReference = providerReference;
        this.providerName = providerName;
        this.amount = amount;
        this.currency = currency;
        this.channel = channel;
        this.paidAt = paidAt;
        this.createdAt = now();
    }
}
