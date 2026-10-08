package com.lagosrent.payment.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.lagosrent.payment.domain.model.PaymentStatus.INITIATED;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;
import static java.time.Instant.now;

@Entity
@Table(name = "payment_attempts")
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;

    @Column(name = "internal_reference", nullable = false, unique = true)
    private String internalReference;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "provider_reference", nullable = false)
    private String providerReference;

    @Column(name = "authorization_url")
    private String authorizationUrl;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "NGN";

    @Enumerated(STRING)
    @Column(nullable = false)
    private PaymentStatus status = INITIATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = now();

    public PaymentAttempt(Payable payable, String internalReference, String idempotencyKey, String providerName, BigDecimal amount, String currency,
                          String authorizationUrl) {
        this.payable = payable;
        this.internalReference = internalReference;
        this.idempotencyKey = idempotencyKey;
        this.providerName = providerName;
        this.amount = amount;
        this.currency = currency;
        this.authorizationUrl = authorizationUrl;
        this.createdAt = now();
        this.updatedAt = now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = now();
    }
}
