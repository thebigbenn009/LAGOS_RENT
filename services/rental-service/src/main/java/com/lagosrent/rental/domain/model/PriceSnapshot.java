package com.lagosrent.rental.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class PriceSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private RentalApplication application;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "listing_version", nullable = false)
    private Long listingVersion;

    @Column(name = "rent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "caution_deposit", nullable = false, precision = 15, scale = 2)
    private BigDecimal cautionDeposit = BigDecimal.ZERO;

    @Column(name = "service_charge", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Column(name = "legal_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal legalFee = BigDecimal.ZERO;

    @Column(name = "total_move_in_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalMoveInAmount;

    @Column(nullable = false)
    private String currency = "NGN";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fee_breakdown", columnDefinition = "jsonb", nullable = false)
    private String feeBreakdown;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt = Instant.now();

    public PriceSnapshot(RentalApplication application, UUID listingId, Long listingVersion,
                         BigDecimal rentAmount, BigDecimal cautionDeposit, BigDecimal serviceCharge,
                         BigDecimal legalFee, BigDecimal totalMoveInAmount, String feeBreakdownJson) {
        this.application = application;
        this.listingId = listingId;
        this.listingVersion = listingVersion;
        this.rentAmount = rentAmount;
        this.cautionDeposit = cautionDeposit != null ? cautionDeposit : BigDecimal.ZERO;
        this.serviceCharge = serviceCharge != null ? serviceCharge : BigDecimal.ZERO;
        this.legalFee = legalFee != null ? legalFee : BigDecimal.ZERO;
        this.totalMoveInAmount = totalMoveInAmount;
        this.currency = "NGN";
        this.feeBreakdown = feeBreakdownJson;
        this.capturedAt = Instant.now();
    }


}
