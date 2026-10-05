package com.lagosrent.rental.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ApproveInspectionRequest(
        @NotNull(message = "Confirmed datetime is required")
        Instant confirmedDatetime
) {}
