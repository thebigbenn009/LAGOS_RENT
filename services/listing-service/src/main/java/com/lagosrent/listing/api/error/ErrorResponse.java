package com.lagosrent.listing.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ErrorResponse", description = "RFC 7807 problem details error payload")
public record ErrorResponse(
        @Schema(description = "URI reference identifying the problem type", example = "https://api.lagosrent.com/errors/RESOURCE_NOT_FOUND")
        String type,

        @Schema(description = "Short human-readable summary of problem", example = "Resource Not Found")
        String title,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "Detailed explanation specific to this occurrence", example = "Listing with ID 3fa85f64-5717-4562-b3fc-2c963f66afa6 not found")
        String detail,

        @Schema(description = "URI path of the resource where error occurred", example = "/api/v1/listings/3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String instance,

        @Schema(description = "Internal application error code", example = "RESOURCE_NOT_FOUND")
        String code,

        @Schema(description = "Timestamp when error occurred", example = "2026-10-03T16:00:00Z")
        Instant time,

        @Schema(description = "Correlation ID for request distributed tracing", example = "c0a80123-8b7a-40d2-9842-fa331165bc92")
        String correlationId
) {
    public static ErrorResponse of(int status, String title, String detail, String code, String instance, String correlationId) {
        return new ErrorResponse(
                "https://api.lagosrent.com/errors/" + code,
                title,
                status,
                detail,
                instance,
                code,
                Instant.now(),
                correlationId
        );
    }
}
