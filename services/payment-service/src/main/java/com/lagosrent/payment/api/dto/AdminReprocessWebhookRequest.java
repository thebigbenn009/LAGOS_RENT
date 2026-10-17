package com.lagosrent.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReprocessWebhookRequest(
        @NotBlank(message = "Mandatory audit reason must be supplied for webhook reprocessing")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason
) {}
