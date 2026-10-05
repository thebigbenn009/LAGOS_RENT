package com.lagosrent.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateLandlordProfileRequest", description = "Payload for updating landlord business and banking disbursement details")
public record UpdateLandlordProfileRequest(
        @Schema(description = "Registered business / corporate name", example = "Bakare Real Estate Ltd")
        @Size(max = 255, message = "Business name cannot exceed 255 characters")
        String businessName,

        @Schema(description = "Account holder name matching bank records", example = "Tunde Bakare")
        @Size(max = 255, message = "Bank account name cannot exceed 255 characters")
        String bankAccountName,

        @Schema(description = "10-digit NUBAN bank account number", example = "0123456789")
        @Size(max = 50, message = "Bank account number cannot exceed 50 characters")
        String bankAccountNumber,

        @Schema(description = "Name of commercial bank in Nigeria", example = "Zenith Bank")
        @Size(max = 100, message = "Bank name cannot exceed 100 characters")
        String bankName
) {
}
