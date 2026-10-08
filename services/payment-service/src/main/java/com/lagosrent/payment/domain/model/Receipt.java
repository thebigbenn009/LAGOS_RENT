package com.lagosrent.payment.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static java.time.Instant.now;
import static org.hibernate.type.SqlTypes.JSON;


@Entity
@Table(name = "receipts")
@Getter
@Setter
@NoArgsConstructor
public class Receipt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private PaymentTransaction transaction;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;

    @Column(name = "receipt_number", nullable = false, unique = true)
    private String receiptNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "NGN";

    @Column(name = "issued_to_tenant_keycloak_id", nullable = false)
    private String issuedToTenantKeycloakId;

    @Column(name = "issued_for_landlord_keycloak_id", nullable = false)
    private String issuedForLandlordKeycloakId;

    @JdbcTypeCode(JSON)
    @Column(name = "receipt_data", columnDefinition = "jsonb", nullable = false)
    private String receiptData;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = now();

    public Receipt(PaymentTransaction transaction, Payable payable, String receiptNumber,
                   BigDecimal amount, String currency, String issuedToTenantKeycloakId,
                   String issuedForLandlordKeycloakId, String receiptDataJson) {
        this.transaction = transaction;
        this.payable = payable;
        this.receiptNumber = receiptNumber;
        this.amount = amount;
        this.currency = currency;
        this.issuedToTenantKeycloakId = issuedToTenantKeycloakId;
        this.issuedForLandlordKeycloakId = issuedForLandlordKeycloakId;
        this.receiptData = receiptDataJson;
        this.issuedAt = Instant.now();
    }
}
