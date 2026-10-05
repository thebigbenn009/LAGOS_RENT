package com.lagosrent.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "AdminReviewVerificationRequest", description = "Payload for approving or rejecting a landlord verification document")
public record AdminReviewVerificationRequest(
        @Schema(description = "Admin notes or rejection rationale explaining verification decision", example = "Government-issued ID successfully cross-checked against NIMC database")
        @NotBlank(message = "Reason/notes are mandatory for review decisions")
        String reason
) {
}
