package com.lagosrent.rental.domain.model;

public enum SagaStatus {
    STARTED,
    RESERVATION_REQUESTED,
    RESERVED,
    PAYMENT_PENDING,
    COMPLETED,
    FAILED,
    REVIEW_REQUESTED
}
