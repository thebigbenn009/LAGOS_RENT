package com.lagosrent.listing.application.service;

import com.lagosrent.listing.api.dto.CreatePropertyRequest;
import com.lagosrent.listing.api.dto.CreateRentalUnitRequest;
import com.lagosrent.listing.api.dto.PropertyResponse;
import com.lagosrent.listing.api.dto.RentalUnitResponse;
import com.lagosrent.listing.application.exception.ResourceNotFoundException;
import com.lagosrent.listing.domain.model.Property;
import com.lagosrent.listing.domain.model.RentalUnit;
import com.lagosrent.listing.infrastructure.persistence.PropertyRepository;
import com.lagosrent.listing.infrastructure.persistence.RentalUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lagosrent.listing.domain.model.UnitType.APARTMENT;
import static com.lagosrent.listing.domain.model.UnitType.FLAT;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyService - Property & Rental Unit Ownership Unit Tests")
public class PropertyServiceTest {
    // Verified seeded landlord identity from CLAUDE.md
    private static final String OWNER_LANDLORD_ID = "818746e8-5729-4aa5-91fc-a99199dbd2aa";
    private static final String OTHER_LANDLORD_ID = "99999999-0000-4aa5-91fc-ffffffffffff";

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private RentalUnitRepository rentalUnitRepository;

    @Captor
    private ArgumentCaptor<Property> propertyCaptor;

    @Captor
    private ArgumentCaptor<RentalUnit> rentalUnitCaptor;

    private PropertyService propertyService;

    private UUID propertyId;
    private Property existingProperty;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(propertyRepository, rentalUnitRepository);

        propertyId = randomUUID();
        existingProperty = new Property(
                OWNER_LANDLORD_ID,
                "Admiralty Heights",
                "Eti-Osa",
                "Lekki Phase 1",
                "Plot 14 Admiralty Way",
                "Beside Daytona Supermarket"
        );
        ReflectionTestUtils.setField(existingProperty, "id", propertyId);
    }

    // =========================================================================
    // 1. createProperty
    // =========================================================================
    @Nested
    @DisplayName("createProperty")
    class CreateProperty {
        @Test
        @DisplayName("Persists Property owned by the caller with Lagos state default and optional coordinates")
        void persistPropertyForAuthenticatedLandlord() {
            // given
            CreatePropertyRequest request = new CreatePropertyRequest(
                    "Silvercrest Court",
                    "Eti-Osa",
                    "Lekki Phase 1",
                    "14 Admiralty Way",
                    "Opposite Lekki Coliseum",
                    "Near Admiralty Toll Gate",
                    new BigDecimal("6.4474000"),
                    new BigDecimal("3.4723000")
            );

            UUID generatedId = randomUUID();
            given(propertyRepository.save(any(Property.class))).willAnswer(invocation -> {
                Property saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", generatedId);
                return saved;
            });

            // when
            PropertyResponse response = propertyService.createProperty(OWNER_LANDLORD_ID, request);

            // then
            then(propertyRepository).should().save(propertyCaptor.capture());
            Property captured = propertyCaptor.getValue();

            assertThat(captured.getLandlordKeycloakId()).isEqualTo(OWNER_LANDLORD_ID);
            assertThat(captured.getState()).isEqualTo("Lagos");
            assertThat(captured.getTitle()).isEqualTo("Silvercrest Court");
            assertThat(captured.getLga()).isEqualTo("Eti-Osa");
            assertThat(captured.getArea()).isEqualTo("Lekki Phase 1");
            assertThat(captured.getAddressLine1()).isEqualTo("14 Admiralty Way");
            assertThat(captured.getAddressLine2()).isEqualTo("Opposite Lekki Coliseum");
            assertThat(captured.getLandmark()).isEqualTo("Near Admiralty Toll Gate");
            assertThat(captured.getLatitude()).isEqualByComparingTo("6.4474000");
            assertThat(captured.getLongitude()).isEqualByComparingTo("3.4723000");

            assertThat(response.id()).isEqualTo(generatedId);
            assertThat(response.landlordKeycloakId()).isEqualTo(OWNER_LANDLORD_ID);
            assertThat(response.state()).isEqualTo("Lagos");
        }
    }

    // =========================================================================
    // 2. getPropertyById & getLandlordProperties
    // =========================================================================
    @Nested
    @DisplayName("getPropertyById & getLandlordProperties")
    class ReadProperties {

        @Test
        @DisplayName("getPropertyById returns mapped PropertyResponse when property exists")
        void returnsPropertyWhenFound() {
            // given
            given(propertyRepository.findById(propertyId)).willReturn(Optional.of(existingProperty));

            // when
            PropertyResponse response = propertyService.getPropertyById(propertyId);

            // then
            assertThat(response.id()).isEqualTo(propertyId);
            assertThat(response.landlordKeycloakId()).isEqualTo(OWNER_LANDLORD_ID);
            assertThat(response.title()).isEqualTo("Admiralty Heights");
        }
    }

    @Test
    @DisplayName("getPropertyById throws ResourceNotFoundException when property does not exist")
    void throwsResourceNotFoundWhenPropertyMissing() {
        // given
        UUID unknownId = randomUUID();
        given(propertyRepository.findById(unknownId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> propertyService.getPropertyById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("getLandlordProperties returns only properties belonging to the specified landlord")
    void returnsPropertiesForLandlord() {
        // given
        given(propertyRepository.findByLandlordKeycloakId(OWNER_LANDLORD_ID))
                .willReturn(List.of(existingProperty));

        // when
        List<PropertyResponse> results = propertyService.getLandlordProperties(OWNER_LANDLORD_ID);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo(propertyId);
        assertThat(results.getFirst().landlordKeycloakId()).isEqualTo(OWNER_LANDLORD_ID);
    }

    // =========================================================================
    // 3. addRentalUnit
    // =========================================================================
    @Nested
    @DisplayName("addRentalUnit Ownership & Uniqueness Rules")
    class AddRentalUnit {
        private final CreateRentalUnitRequest validUnitRequest = new CreateRentalUnitRequest(
                "Flat 2B",
                APARTMENT,
                3,
                3,
                4,
                new BigDecimal("145.50"),
                2
        );

        @Test
        @DisplayName("persists RentalUnit linked to parent Property when caller owns property and unitNumber is unique")
        void persistUnitWhenCallerOwnsPropertyAndWhenUnitNumberIsUnique() {
            // given
            UUID generatedUnitId = randomUUID();
            given(propertyRepository.findById(propertyId)).willReturn(Optional.of(existingProperty));
            given(rentalUnitRepository.findByPropertyIdAndUnitNumber(propertyId, "Flat 2B"))
                    .willReturn(Optional.empty());
            given(rentalUnitRepository.save(any(RentalUnit.class))).willAnswer(invocation -> {
                RentalUnit unit = invocation.getArgument(0);
                ReflectionTestUtils.setField(unit, "id", generatedUnitId);
                return unit;
            });

            // when
            RentalUnitResponse response = propertyService.addRentalUnit(
                    propertyId,
                    OWNER_LANDLORD_ID,
                    validUnitRequest
            );

            // then
            then(rentalUnitRepository).should().save(rentalUnitCaptor.capture());
            RentalUnit captured = rentalUnitCaptor.getValue();

            assertThat(captured.getProperty()).isSameAs(existingProperty);
            assertThat(captured.getUnitNumber()).isEqualTo("Flat 2B");
            assertThat(captured.getUnitType()).isEqualTo(APARTMENT);
            assertThat(captured.getBedrooms()).isEqualTo(3);
            assertThat(captured.getBathrooms()).isEqualTo(3);
            assertThat(captured.getToilets()).isEqualTo(4);
            assertThat(captured.getSizeSqMeters()).isEqualByComparingTo("145.50");
            assertThat(captured.getFloorNumber()).isEqualTo(2);

            assertThat(response.id()).isEqualTo(generatedUnitId);
            assertThat(response.propertyId()).isEqualTo(propertyId);
            assertThat(response.unitNumber()).isEqualTo("Flat 2B");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException and saves nothing when parent Property does not exist")
        void throwsNotFoundWhenPropertyDoesNotExist() {
            // given
            UUID missingPropertyId = randomUUID();
            given(propertyRepository.findById(missingPropertyId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> propertyService.addRentalUnit(
                    missingPropertyId,
                    OWNER_LANDLORD_ID,
                    validUnitRequest
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(missingPropertyId.toString());

            then(rentalUnitRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("rejects with IllegalArgumentException and saved nothing when caller does not own the Property")
        void rejectsWhenCallerDoesNotOwnProperty() {
            // given
            given(propertyRepository.findById(propertyId)).willReturn(Optional.of(existingProperty));

            // when / then
            assertThatThrownBy(() -> propertyService.addRentalUnit(
                    propertyId,
                    OTHER_LANDLORD_ID,
                    validUnitRequest
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You do not own this property");

            then(rentalUnitRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("rejects duplicate unitNumber within the same Property with IllegalStateException and saves nothing")
        void rejectsDuplicateUnitNumberWithinSameProperty() {
            // given
            RentalUnit existingUnit = new RentalUnit(
                    existingProperty,
                    "Flat 2B",
                    APARTMENT,
                    3,
                    3,
                    4
            );
            given(propertyRepository.findById(propertyId)).willReturn(Optional.of(existingProperty));
            given(rentalUnitRepository.findByPropertyIdAndUnitNumber(propertyId, "Flat 2B"))
                    .willReturn(Optional.of(existingUnit));

            // when / then
            assertThatThrownBy(() -> propertyService.addRentalUnit(
                    propertyId,
                    OWNER_LANDLORD_ID,
                    validUnitRequest
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unit number 'Flat 2B' already exists in this property");

            then(rentalUnitRepository).should(never()).save(any());
        }
    }

    // =========================================================================
    // 4. getPropertyUnits
    // =========================================================================
    @Nested
    @DisplayName("getPropertyUnits")
    class GetPropertyUnits {

        @Test
        @DisplayName("returns mapped RentalUnitResponse list when parent Property exists")
        void returnsUnitsWhenPropertyExists() {
            // given
            UUID unitId = randomUUID();
            RentalUnit unit = new RentalUnit(existingProperty, "Flat 1A", FLAT, 2, 2, 3);
            ReflectionTestUtils.setField(unit, "id", unitId);

            given(propertyRepository.existsById(propertyId)).willReturn(true);
            given(rentalUnitRepository.findByPropertyId(propertyId)).willReturn(List.of(unit));

            // when
            List<RentalUnitResponse> units = propertyService.getPropertyUnits(propertyId);

            // then
            assertThat(units).hasSize(1);
            assertThat(units.getFirst().id()).isEqualTo(unitId);
            assertThat(units.getFirst().propertyId()).isEqualTo(propertyId);
            assertThat(units.getFirst().unitNumber()).isEqualTo("Flat 1A");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when parent Property does not exist")
        void throwsNotFoundWhenPropertyDoesNotExist() {
            // given
            UUID unknownPropertyId = randomUUID();
            given(propertyRepository.existsById(unknownPropertyId)).willReturn(false);

            // when / then
            assertThatThrownBy(() -> propertyService.getPropertyUnits(unknownPropertyId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknownPropertyId.toString());

            then(rentalUnitRepository).shouldHaveNoInteractions();
        }
    }
}
