package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.LandlordProfile;
import com.lagosrent.account.domain.model.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "LandlordProfileResponse", description = "Landlord business, settlement bank details, and KYC verification status")
public record LandlordProfileResponse(
        @Schema(description = "Landlord profile ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID id,

        @Schema(description = "Associated Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID accountId,

        @Schema(description = "KYC Verification Status", example = "VERIFIED")
        VerificationStatus verificationStatus,

        @Schema(description = "Registered business / corporate name (if applicable)", example = "Bakare Real Estate Ltd")
        String businessName,

        @Schema(description = "Account holder name for rent disbursements", example = "Tunde Bakare")
        String bankAccountName,

        @Schema(description = "10-digit NUBAN account number", example = "0123456789")
        String bankAccountNumber,

        @Schema(description = "Commercial bank name in Nigeria", example = "Zenith Bank")
        String bankName
) {
    public static LandlordProfileResponse from(LandlordProfile profile) {
        return new LandlordProfileResponse(
                profile.getId(),
                profile.getAccount().getId(),
                profile.getVerificationStatus(),
                profile.getBusinessName(),
                profile.getBankAccountName(),
                profile.getBankAccountNumber(),
                profile.getBankName()
        );
    }
}
