package com.lagosrent.account.api.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTenantProfileRequest(
        @Size(max = 255, message = "Occupation cannot exceed 255 characters")
        String occupation,

        @PositiveOrZero(message = "Annual budget must be greater than or equal to zero")
        BigDecimal annualBudget,

        LocalDate moveInDate,
        
        @Size(max = 100, message = "Preferred LGA cannot exceed 100 characters")
        Size preferredLga
) {
}
