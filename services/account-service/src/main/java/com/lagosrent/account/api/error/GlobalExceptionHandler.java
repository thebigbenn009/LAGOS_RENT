package com.lagosrent.account.api.error;

import com.lagosrent.account.application.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                "RESOURCE_NOT_FOUND",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed for request [{}]: {}", request.getRequestURI(), details);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                details,
                "VALIDATION_ERROR",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Request Parameter",
                ex.getMessage(),
                "BAD REQUEST",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        log.warn("Access denied for request [{}]: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to perform this action.",
                "FORBIDDEN",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // Print the full stack trace tagged with the correlation ID to server logs
        log.error("Unhandled internal server error on [{}], correlationId [{}]:",
                request.getRequestURI(), correlationId, ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid State Transition",
                ex.getMessage(),
                "INVALID_STATE_TRANSITION",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        // Print the full stack trace tagged with the correlation ID to server logs
        log.error("Unhandled internal server error on [{}], correlationId [{}]:",
                request.getRequestURI(), correlationId, ex);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occured. Please contact support.",
                "INTERNAL_SERVER_ERROR",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
