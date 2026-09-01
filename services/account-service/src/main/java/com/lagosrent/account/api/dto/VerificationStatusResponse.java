package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.VerificationStatus;

import java.util.List;

public record VerificationStatusResponse(
        VerificationStatus verificationStatus,
        List<VerificationStatusResponse> cases
) {
}
