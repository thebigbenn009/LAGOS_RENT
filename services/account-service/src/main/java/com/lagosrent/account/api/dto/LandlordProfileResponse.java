package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.LandlordProfile;
import com.lagosrent.account.domain.model.VerificationStatus;

import java.util.UUID;

public record LandlordProfileResponse(
        UUID id,
        UUID accountId,
        VerificationStatus verificationStatus,
        String businessName,
        String bankAccountName,
        String bankAccountNumber,
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
