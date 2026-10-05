package com.lagosrent.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "AdminSuspendAccountRequest", description = "Payload for suspending or reactivating a user account")
public record AdminSuspendAccountRequest(
        @Schema(description = "Detailed justification/audit reason for account status change", example = "Account suspended following confirmed rental fraud reports")
        @NotBlank(message = "Reason is mandatory when suspending or reactivating an account")
        String reason
) {
}
