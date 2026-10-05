package com.lagosrent.rental.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DecideApplicationRequest(
        @NotBlank(message = "Reason/notes are required")
        String reason
) {
}
