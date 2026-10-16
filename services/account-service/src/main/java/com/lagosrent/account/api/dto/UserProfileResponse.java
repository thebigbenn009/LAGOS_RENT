package com.lagosrent.account.api.dto;

public record UserProfileResponse(
        String keycloakId,
        String email,
        String fullName,
        String phoneNumber
) {
}
