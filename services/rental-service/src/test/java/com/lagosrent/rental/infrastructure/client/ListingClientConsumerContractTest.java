package com.lagosrent.rental.infrastructure.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.lagosrent.rental.infrastructure.client.ListingClient.ListingDetailDto;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@WireMockTest
@DisplayName("Layer 3: HTTP Consumer Contract - Rental Service ListingClient")
public class ListingClientConsumerContractTest {

    private ListingClient listingClient;
    private final UUID listingId = fromString("fa3f3a60-5a84-4514-8731-12db97eff937");

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // Point the client to Wiremock's ephemeral local port
        String mockBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        this.listingClient = new ListingClient(mockBaseUrl);
    }

    @Test
    @DisplayName("Consumer contract: Valid listing detail response is correctly deserialized")
    void listingDetailMatchesContract() {
        // 1. Arrange: Define the contract stub agreed upon with listing service
        String contractResponseBody = """
                 {
                              "id": "fa3f3a60-5a84-4514-8731-12db97eff937",
                              "rentalUnitId": "550e8400-e29b-41d4-a716-446655440003",
                              "landlordKeycloakId": "landlord-user-999",
                              "title": "Luxury 2-Bedroom Flat in Lekki Phase 1",
                              "description": "Serviced apartment with standby generator",
                              "status": "PUBLISHED",
                              "rentAmount": 3500000.00,
                              "rentPeriod": "YEARLY",
                              "moveInTotal": 4200000.00,
                              "availableFrom": "2026-12-01",
                              "state": "Lagos",
                              "lga": "Eti-Osa",
                              "area": "Lekki Phase 1",
                              "landmark": "Near Admiralty Toll Gate",
                              "version": 1,
                              "fees": [
                                {
                                  "id": "550e8400-e29b-41d4-a716-446655440000",
                                  "feeType": "CAUTION_DEPOSIT",
                                  "amount": 300000.00,
                                  "recipient": "LANDLORD",
                                  "frequency": "ONE_TIME",
                                  "isRefundable": true,
                                  "explanation": "Refundable security caution deposit",
                                  "approvedByAdmin": true
                                },
                                {
                                  "id": "550e8400-e29b-41d4-a716-446655440001",
                                  "feeType": "LEGAL_FEE",
                                  "amount": 400000.00,
                                  "recipient": "LANDLORD",
                                  "frequency": "ONE_TIME",
                                  "isRefundable": false,
                                  "explanation": "Tenancy legal drafting fee",
                                  "approvedByAdmin": true
                                }
                              ]
                            }
                """;
        stubFor(get(urlEqualTo("/api/v1/listings/" + listingId))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(contractResponseBody)
                )
        );
        // 2. Call the client method
        ListingDetailDto dto = listingClient.getListing(listingId);

        // 3. Assert: Verify every domain attribute required by Rental Service
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(listingId);
        assertThat(dto.landlordKeycloakId()).isEqualTo("landlord-user-999");
        assertThat(dto.title()).isEqualTo("Luxury 2-Bedroom Flat in Lekki Phase 1");
        assertThat(dto.status()).isEqualTo("PUBLISHED");
        assertThat(dto.rentAmount()).isEqualByComparingTo("3500000.00");
        assertThat(dto.rentPeriod()).isEqualTo("YEARLY");
        assertThat(dto.moveInTotal()).isEqualByComparingTo("4200000.00");
        assertThat(dto.version()).isEqualTo(1L);

        // Verify fee breakdown mapping for both fees
        assertThat(dto.fees()).hasSize(2);

        // Fee 1: Caution Deposit (Refundable)
        ListingClient.ListingFeeDto caution = dto.fees().getFirst();
        assertThat(caution.feeType()).isEqualTo("CAUTION_DEPOSIT");
        assertThat(caution.amount()).isEqualByComparingTo("300000.00");
        assertThat(caution.recipient()).isEqualTo("LANDLORD");
        assertThat(caution.frequency()).isEqualTo("ONE_TIME");
        assertThat(caution.isRefundable()).isTrue();
        assertThat(caution.explanation()).isEqualTo("Refundable security caution deposit");

        // Fee 2: Legal Fee (Non-refundable)
        ListingClient.ListingFeeDto legal = dto.fees().get(1);
        assertThat(legal.feeType()).isEqualTo("LEGAL_FEE");
        assertThat(legal.amount()).isEqualByComparingTo("400000.00");
        assertThat(legal.recipient()).isEqualTo("LANDLORD");
        assertThat(legal.frequency()).isEqualTo("ONE_TIME");
        assertThat(legal.isRefundable()).isFalse();
        assertThat(legal.explanation()).isEqualTo("Tenancy legal drafting fee");
    }

    @Test
    @DisplayName("Consumer contract: HTTP 404 throws HttpClientErrorException.NotFound")
    void missingListingThrowsNotFound() {
        stubFor(get(urlEqualTo("/api/v1/listings/" + listingId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "timestamp": "2026-11-02T12:00:00Z",
                                   "status": 404,
                                   "error": "Not Found",
                                   "message": "Listing not found: fa3f3a60-5a84-4514-8731-12db97eff937",
                                   "path": "/api/v1/listings/fa3f3a60-5a84-4514-8731-12db97eff937"
                                 }
                                """))
        );
        assertThatThrownBy(() -> listingClient.getListing(listingId))
                .isInstanceOf(HttpClientErrorException.NotFound.class);


    }
}
