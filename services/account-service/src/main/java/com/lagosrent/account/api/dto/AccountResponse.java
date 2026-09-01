package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.Account;
import com.lagosrent.account.domain.model.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String keycloakUserId,
        String email,
        String firstName,
        String lastName,
        String phone,
        AccountStatus status,
        Instant createdAt,
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
