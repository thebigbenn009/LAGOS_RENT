package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.FeeFrequency;
import com.lagosrent.listing.domain.model.FeeRecipient;
import com.lagosrent.listing.domain.model.FeeType;
import com.lagosrent.listing.domain.model.ListingFee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingFeeDto(
        UUID id,

        @NotNull @Positive(message = "Fee amount must be greater than zero")
        FeeType feeType,

        @NotNull @Positive(message = "Fee amount must be greater than zero")
        BigDecimal amount,
        
        @NotNull(message = "Recipient is required")
        FeeRecipient recipient,

        @NotNull(message = "Frequency is required")
        FeeFrequency frequency,

        boolean isRefundable,

        @NotBlank(message = "Explanation is required for fee transparency")
        String explanation,

        boolean approvedByAdmin
) {
    public static ListingFeeDto from(ListingFee fee) {
        return new ListingFeeDto(
                fee.getId(),
                fee.getFeeType(),
                fee.getAmount(),
                fee.getRecipient(),
                fee.getFrequency(),
                fee.isRefundable(),
                fee.getExplanation(),
                fee.isApprovedByAdmin()
        );
    }
}
