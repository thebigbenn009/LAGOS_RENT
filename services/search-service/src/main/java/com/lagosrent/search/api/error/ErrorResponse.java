package com.lagosrent.search.api.error;

import java.time.Instant;

public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        Instant time,
        String correlationId
) {
    public static ErrorResponse of(int status, String title, String detail,
                                   String code, String instance,
                                   String correlationId) {
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
