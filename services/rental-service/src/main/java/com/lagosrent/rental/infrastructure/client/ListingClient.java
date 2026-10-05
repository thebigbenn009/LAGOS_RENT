package com.lagosrent.rental.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class ListingClient {
    private final RestClient restClient;

    public ListingClient(@Value("${listing-service.url:http://localhost:8082}") String listingServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(listingServiceUrl)
                .build();
    }

    public ListingDetailDto getListing(UUID listingId) {
        return restClient.get()
                .uri("/api/v1/listings/{listingId}", listingId)
                .retrieve()
                .body(ListingDetailDto.class);
    }

    public record ListingDetailDto(
            UUID id,
            String landlordKeycloakId,
            String title,
            String status,
            BigDecimal rentAmount,
            String rentPeriod,
            BigDecimal moveInTotal,
            Long version,
            List<ListingFeeDto> fees
    ) {}

    public record ListingFeeDto(
            String feeType,
            BigDecimal amount,
            String recipient,
            String frequency,
            Boolean isRefundable,
            String explanation
    ) {}
}
