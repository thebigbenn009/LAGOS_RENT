package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.Account;
import com.lagosrent.account.domain.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "AccountResponse", description = "User core account information synced from Keycloak")
public record AccountResponse(
        @Schema(description = "Account internal database ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Keycloak Subject UUID", example = "b845df0e-862d-45db-9c3f-c30a4be15dfa")
        String keycloakUserId,

        @Schema(description = "User primary email address", example = "tunde.bakare@example.ng")
        String email,

        @Schema(description = "User first name", example = "Tunde")
        String firstName,

        @Schema(description = "User last name / surname", example = "Bakare")
        String lastName,

        @Schema(description = "Nigerian phone number (E.164 or national format)", example = "+2348023456789")
        String phone,

        @Schema(description = "Account lifecycle status", example = "ACTIVE")
        AccountStatus status,

        @Schema(description = "Account creation timestamp", example = "2026-09-01T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Account last update timestamp", example = "2026-09-02T12:00:00Z")
        Instant updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getKeycloakUserId(),
                account.getEmail(),
                account.getFirstName(),
                account.getLastName(),
                account.getPhone(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
