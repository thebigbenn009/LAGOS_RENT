package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.TenantProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TenantProfileResponse(
        UUID id,
        UUID accountId,
        String occupation,
        BigDecimal annualBudget,
        LocalDate moveInDate,
        String preferredLga
) {
    public static TenantProfileResponse from(TenantProfile profile) {
        return new TenantProfileResponse(
                profile.getId(),
                profile.getAccount().getId(),
                profile.getOccupation(),
                profile.getAnnualBudget(),
                profile.getMoveInDate(),
                profile.getPreferredLga()
        );
    }
}
