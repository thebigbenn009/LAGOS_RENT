package com.lagosrent.account.api.dto;

import com.lagosrent.account.domain.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitVerificationCaseRequest(
        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Document S3 key is required")
        @Size(max = 500, message = "Document S3 key cannot exceed 500 characters")
        String documentS3Key,

        @NotBlank(message = "Document filename is required")
        @Size(max = 255, message = "Document filename cannot exceed 255 characters")
        String documentFileName
) {
}
