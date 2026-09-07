package com.lagosrent.listing.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_fees")
@Getter
@Setter
@NoArgsConstructor
public class ListingFee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false)
    private FeeType feeType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeRecipient recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeFrequency frequency = FeeFrequency.ONE_OFF;

    @Column(name = "is_refundable", nullable = false)
    private boolean isRefundable = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "approved_by_admin", nullable = false)
    private boolean approvedByAdmin = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ListingFee(Listing listing, FeeType feeType, BigDecimal amount, FeeRecipient recipient, FeeFrequency frequency, boolean isRefundable,
                      String explanation) {
        this.listing = listing;
        this.feeType = feeType;
        this.amount = amount;
        this.recipient = recipient;
        this.frequency = frequency;
        this.isRefundable = isRefundable;
        this.explanation = explanation;
        this.approvedByAdmin = (feeType != FeeType.OTHER);
        this.createdAt = Instant.now();
    }
}
