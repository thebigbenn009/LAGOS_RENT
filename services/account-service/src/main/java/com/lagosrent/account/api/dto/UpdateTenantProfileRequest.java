package com.lagosrent.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "UpdateTenantProfileRequest", description = "Payload for updating tenant preferences and rental criteria")
public record UpdateTenantProfileRequest(
        @Schema(description = "Profession / Occupation", example = "Software Engineer")
        @Size(max = 255, message = "Occupation cannot exceed 255 characters")
        String occupation,

        @Schema(description = "Maximum annual rent budget in NGN", example = "4000000.00")
        @PositiveOrZero(message = "Annual budget must be greater than or equal to zero")
        BigDecimal annualBudget,

        @Schema(description = "Target move-in date", example = "2026-11-01")
        LocalDate moveInDate,

        @Schema(description = "Preferred Lagos Local Government Area", example = "Eti-Osa")
        @Size(max = 100, message = "Preferred LGA cannot exceed 100 characters")
        String preferredLga
) {
}
