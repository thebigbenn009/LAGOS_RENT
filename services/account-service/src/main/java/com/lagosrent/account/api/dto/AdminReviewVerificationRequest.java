package com.lagosrent.account.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminReviewVerificationRequest(
        @NotBlank(message = "Reason/notes are mandatory for review decisions")
        String reason
) {
}
