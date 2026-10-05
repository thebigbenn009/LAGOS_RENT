package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "SubmitVerificationCaseRequest", description = "Payload for submitting KYC identity or property ownership documents for landlord verification")
public record SubmitVerificationCaseRequest(
        @Schema(description = "Verification document category", example = "NIN")
        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @Schema(description = "S3 object storage key where document was uploaded", example = "verification/550e8400-e29b-41d4-a716-446655440002/nin_slip.pdf")
        @NotBlank(message = "Document S3 key is required")
        @Size(max = 500, message = "Document S3 key cannot exceed 500 characters")
        String documentS3Key,

        @Schema(description = "Original filename of uploaded document", example = "nin_slip.pdf")
        @NotBlank(message = "Document filename is required")
        @Size(max = 255, message = "Document filename cannot exceed 255 characters")
        String documentFileName
) {
}
