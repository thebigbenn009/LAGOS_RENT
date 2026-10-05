package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.TenantProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "TenantProfileResponse", description = "Tenant preference and search criteria profile")
public record TenantProfileResponse(
        @Schema(description = "Tenant profile ID", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID id,

        @Schema(description = "Associated Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID accountId,

        @Schema(description = "Profession / Occupation", example = "Software Engineer")
        String occupation,

        @Schema(description = "Maximum annual rent budget in NGN", example = "4000000.00")
        BigDecimal annualBudget,

        @Schema(description = "Target move-in date", example = "2026-11-01")
        LocalDate moveInDate,

        @Schema(description = "Preferred Lagos Local Government Area", example = "Eti-Osa")
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
