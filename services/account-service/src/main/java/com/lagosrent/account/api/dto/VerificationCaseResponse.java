package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.DocumentType;
import com.lagosrent.account.domain.model.VerificationCase;
import com.lagosrent.account.domain.model.VerificationCaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "VerificationCaseResponse", description = "Details of a submitted landlord KYC verification document case")
public record VerificationCaseResponse(
        @Schema(description = "Verification case unique ID", example = "550e8400-e29b-41d4-a716-446655440003")
        UUID id,

        @Schema(description = "Associated landlord profile ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID landlordProfileId,

        @Schema(description = "Type of document submitted", example = "NIN")
        DocumentType documentType,

        @Schema(description = "Document S3 storage key", example = "verification/550e8400-e29b-41d4-a716-446655440002/nin_slip.pdf")
        String documentS3Key,

        @Schema(description = "Uploaded filename", example = "nin_slip.pdf")
        String documentFilename,

        @Schema(description = "Verification case adjudication status", example = "APPROVED")
        VerificationCaseStatus status,

        @Schema(description = "Admin review comments or rejection reason", example = "Document verified successfully against official records")
        String adminNotes,

        @Schema(description = "Document submission timestamp", example = "2026-09-02T09:00:00Z")
        Instant submittedAt,

        @Schema(description = "Admin review timestamp", example = "2026-09-02T11:30:00Z")
        Instant reviewedAt,

        @Schema(description = "Keycloak Subject UUID of reviewing admin", example = "admin-user-789")
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
