package com.lagosrent.account.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateLandlordProfileRequest(
        @Size(max = 255, message = "Business name cannot exceed 255 characters")
        String businessName,

        @Size(max = 255, message = "Bank account name cannot exceed 255 characters")
        String bankAccountName,

        @Size(max = 50, message = "Bank account number cannot exceed 50 characters")
        String bankAccountNumber,

        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        String bankName
) {
}
