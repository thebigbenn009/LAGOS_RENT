package com.lagosrent.account.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminSuspendAccountRequest(
        @NotBlank(message = "Reason is mandatory when suspending or reactivating an account")
        String reason
) {
}
