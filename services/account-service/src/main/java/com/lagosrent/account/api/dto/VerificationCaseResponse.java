package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.DocumentType;
import com.lagosrent.account.domain.model.VerificationCase;
import com.lagosrent.account.domain.model.VerificationCaseStatus;

import java.time.Instant;
import java.util.UUID;

public record VerificationCaseResponse(
        UUID id,
        UUID landlordProfileId,
        DocumentType documentType,
        String documentS3Key,
        String documentFilename,
        VerificationCaseStatus status,
        String adminNotes,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewedBy
) {
    public static VerificationCaseResponse from(VerificationCase vc) {
        return new VerificationCaseResponse(
                vc.getId(),
                vc.getLandlordProfile().getId(),
                vc.getDocumentType(),
                vc.getDocumentS3Key(),
                vc.getDocumentFilename(),
                vc.getStatus(),
                vc.getAdminNotes(),
                vc.getSubmittedAt(),
                vc.getReviewedAt(),
                vc.getReviewedBy()
        );
    }
}
