package com.lagosrent.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRedriveRequest(
        @NotBlank(message = "Mandatory audit reason must be supplied for manual notification redrive")
        @Size(min = 10, max = 500, message = "Reason must provide sufficient explanation (10-500 characters)")
        String reason
) {}
