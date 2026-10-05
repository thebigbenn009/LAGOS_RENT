package com.lagosrent.listing.api.dto;

import com.lagosrent.listing.domain.model.FeeFrequency;
import com.lagosrent.listing.domain.model.FeeRecipient;
import com.lagosrent.listing.domain.model.FeeType;
import com.lagosrent.listing.domain.model.ListingFee;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "ListingFeeDto", description = "Fee component breakdown for a listing (e.g., caution deposit, service charge, agency fee)")
public record ListingFeeDto(
        @Schema(description = "Listing fee ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Type of fee", example = "CAUTION_DEPOSIT")
        @NotNull(message = "Fee type is required")
        FeeType feeType,

        @Schema(description = "Fee amount in NGN", example = "300000.00")
        @NotNull @Positive(message = "Fee amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Entity receiving the fee", example = "LANDLORD")
        @NotNull(message = "Recipient is required")
        FeeRecipient recipient,

        @Schema(description = "Payment frequency of fee", example = "ONE_TIME")
        @NotNull(message = "Frequency is required")
        FeeFrequency frequency,

        @Schema(description = "Whether the fee is refundable at tenancy end", example = "true")
        Boolean isRefundable,

        @Schema(description = "Transparency explanation of fee for tenant", example = "Refundable caution deposit against property damage")
        @NotBlank(message = "Explanation is required for fee transparency")
        String explanation,

        @Schema(description = "Whether this fee was reviewed and approved by admin", example = "true")
        Boolean approvedByAdmin
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
