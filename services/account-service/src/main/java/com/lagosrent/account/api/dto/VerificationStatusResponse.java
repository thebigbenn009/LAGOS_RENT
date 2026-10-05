package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "VerificationStatusResponse", description = "Current landlord KYC status and document submission history")
public record VerificationStatusResponse(
        @Schema(description = "Overall landlord verification status", example = "VERIFIED")
        VerificationStatus verificationStatus,

        @Schema(description = "List of all verification document cases submitted by landlord")
        List<VerificationCaseResponse> cases
) {
}
