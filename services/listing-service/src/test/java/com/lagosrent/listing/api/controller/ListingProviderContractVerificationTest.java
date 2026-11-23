package com.lagosrent.listing.api.controller;

import com.lagosrent.listing.api.dto.ListingFeeDto;
import com.lagosrent.listing.api.dto.ListingResponse;
import com.lagosrent.listing.application.service.ListingService;
import com.lagosrent.listing.domain.model.FeeFrequency;
import com.lagosrent.listing.domain.model.FeeRecipient;
import com.lagosrent.listing.domain.model.FeeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.lagosrent.listing.domain.model.ListingStatus.PUBLISHED;
import static java.time.Instant.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ListingController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Layer 3: HTTP Provider Contract Verification - ListingController")
public class ListingProviderContractVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @Test
    @DisplayName("Provider verification: GET /api/v1/listings/{id} satisfies consumer contract")
    void getListingByIdSatisfiesConsumerContract() throws Exception {
        UUID listingId = fromString("fa3f3a60-5a84-4514-8731-12db97eff937");
        UUID rentalUnitId = fromString("550e8400-e29b-41d4-a716-446655440003");

        ListingFeeDto cautionFee = new ListingFeeDto(
                randomUUID(),
                FeeType.CAUTION_DEPOSIT,
                new BigDecimal("300000.00"),
                FeeRecipient.LANDLORD,
                FeeFrequency.ONE_OFF,
                true,
                "Refundable security caution deposit",
                true
        );

        ListingFeeDto legalFee = new ListingFeeDto(
                randomUUID(),
                FeeType.LEGAL_AND_COUNSEL,
                new BigDecimal("400000.00"),
                FeeRecipient.LANDLORD,
                FeeFrequency.ONE_OFF,
                false,
                "Tenancy legal drafting fee",
                true
        );

        ListingResponse mockResponse = new ListingResponse(
                listingId,
                rentalUnitId,
                "landlord-user-999",
                "Luxury 2-Bedroom Flat in Lekki Phase 1",
                "Serviced apartment with standby generator",
                PUBLISHED,
                new BigDecimal("3500000.00"),
                "YEARLY",
                new BigDecimal("4200000.00"),
                LocalDate.of(2026, 12, 1),
                "Lagos",
                "Eti-Osa",
                "Lekki Phase 1",
                "Near Admiralty Toll Gate",
                1L,
                List.of(cautionFee, legalFee),
                List.of(),
                now(),
                now()
        );

        given(listingService.getListingById(listingId)).willReturn(mockResponse);

        // Perform HTTP request and assert every field mandated by the Consumer Contract
        mockMvc.perform(get("/api/v1/listings/{listingId}", listingId)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                // Mandatory Consumer Contract Fields
                .andExpect(jsonPath("$.id").value("fa3f3a60-5a84-4514-8731-12db97eff937"))
                .andExpect(jsonPath("$.landlordKeycloakId").value("landlord-user-999"))
                .andExpect(jsonPath("$.title").value("Luxury 2-Bedroom Flat in Lekki Phase 1"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.rentAmount").value(3500000.00))
                .andExpect(jsonPath("$.rentPeriod").value("YEARLY"))
                .andExpect(jsonPath("$.moveInTotal").value(4200000.00))
                .andExpect(jsonPath("$.version").value(1))
                // Fee breakdown contract fields
                .andExpect(jsonPath("$.fees").isArray())
                .andExpect(jsonPath("$.fees[0].feeType").value("CAUTION_DEPOSIT"))
                .andExpect(jsonPath("$.fees[0].amount").value(300000.00))
                .andExpect(jsonPath("$.fees[0].isRefundable").value(true))
                .andExpect(jsonPath("$.fees[1].feeType").value("LEGAL_AND_COUNSEL"))
                .andExpect(jsonPath("$.fees[1].amount").value(400000.00))
                .andExpect(jsonPath("$.fees[1].isRefundable").value(false));
    }
}
