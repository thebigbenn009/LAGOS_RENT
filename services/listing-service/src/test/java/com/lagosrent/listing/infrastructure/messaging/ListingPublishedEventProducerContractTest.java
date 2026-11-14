package com.lagosrent.listing.infrastructure.messaging;

import com.lagosrent.listing.infrastructure.messaging.payload.ListingPublishedPayload;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.lagosrent.listing.infrastructure.messaging.payload.ListingPublishedPayload.FeeItemPayload;
import static com.lagosrent.listing.infrastructure.messaging.payload.ListingPublishedPayload.MediaItemPayload;
import static com.networknt.schema.InputFormat.JSON;
import static java.time.Instant.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Layer 3: Event Producer Contract - listing.published.v1 JSON Schema")
public class ListingPublishedEventProducerContractTest {
    private ObjectMapper objectMapper;
    private JsonSchema eventSchema;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Load Canonical Schema from test resources
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        InputStream schemaStream = getClass().getResourceAsStream("/contracts/events/listing.published.v1.schema.json");
        assertThat(schemaStream)
                .as("listing.published.v1.schema.json must exist on test classpath")
                .isNotNull();
        eventSchema = schemaFactory.getSchema(schemaStream);
    }

    @Test
    @DisplayName("Producer contract: Outbox event conforms strictly to listing.published.v1 schema")
    void outboxEventConformsToSchema() throws Exception {
        UUID listingId = fromString("fa3f3a60-5a84-4514-8731-12db97eff937");
        UUID propertyId = fromString("11111111-2222-3333-4444-555555555555");
        UUID unitId = fromString("550e8400-e29b-41d4-a716-446655440003");

        FeeItemPayload fee = new FeeItemPayload(
                "CAUTION_DEPOSIT",
                new BigDecimal("300000.00"),
                "LANDLORD",
                "ONE_OFF",
                true,
                "Refundable deposit"
        );

        MediaItemPayload media = new MediaItemPayload(
                "PHOTO",
                "listings/fa3f3a60/exterior.jpg",
                "exterior.jpg",
                0,
                "Front View",
                true
        );

        ListingPublishedPayload payload = new ListingPublishedPayload(
                listingId,
                "landlord-user-999",
                propertyId,
                unitId,
                "Luxury 2-Bedroom Flat in Lekki Phase 1",
                "Spacious flat with backup inverter",
                new BigDecimal("3500000.00"),
                "YEARLY",
                new BigDecimal("4200000.00"),
                LocalDate.of(2026, 12, 1),
                "Lagos",
                "Eti-Osa",
                "Lekki Phase 1",
                "Near Admiralty Way",
                new BigDecimal("6.4474"),
                new BigDecimal("3.4723"),
                "APARTMENT",
                2,
                2,
                2,
                new BigDecimal("120.5"),
                3,
                List.of(fee),
                List.of(media)
        );

        EventEnvelope<ListingPublishedPayload> envelope = new EventEnvelope<>(
                randomUUID(),
                "listing.published.v1",
                "listing",
                listingId.toString(),
                1L,
                now(),
                "listing-service",
                UUID.randomUUID().toString(),
                null,
                payload
        );

        // Serialize envelope to JSON using Jackson 3
        String eventJson = objectMapper.writeValueAsString(envelope);

        // Validate JSON string directly against schema
        Set<ValidationMessage> validationErrors = eventSchema.validate(eventJson, JSON);

        // Assert zero validation errors
        assertThat(validationErrors)
                .as("Event JSON must have zero schema validation violations: %s", validationErrors)
                .isEmpty();
    }
}
