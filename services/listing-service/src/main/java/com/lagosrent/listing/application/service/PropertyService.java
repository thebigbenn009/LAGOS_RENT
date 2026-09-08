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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;

    @Transactional
    public PropertyResponse createProperty(String landlordKeycloakId, CreatePropertyRequest request) {
        Property property = new Property(
                landlordKeycloakId,
                request.title(),
                request.lga(),
                request.area(),
                request.addressLine1(),
                request.landmark()
        );

        property.setAddressLine2(request.addressLine2());
        property.setLatitude(request.latitude());
        property.setLongitude(request.longitude());

        return PropertyResponse.from(propertyRepository.save(property));
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> getLandlordProperties(String landlordKeycloakId) {
        return propertyRepository.findByLandlordKeycloakId(landlordKeycloakId).stream().map(PropertyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        return PropertyResponse.from(property);
    }

    @Transactional
    public RentalUnitResponse addRentalUnit(UUID propertyId, String landlordKeycloakId, CreateRentalUnitRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));

        if (!property.getLandlordKeycloakId().equals(landlordKeycloakId)) {
            throw new IllegalArgumentException("You do not own this property");
        }

        RentalUnit unit = new RentalUnit(
                property,
                request.unitNumber(),
                request.unitType(),
                request.bedrooms(),
                request.bathrooms(),
                request.toilets()
        );
        unit.setSizeSqMeters(request.sizeSqMeters());
        unit.setFloorNumber(request.floorNumber());

        return RentalUnitResponse.from(rentalUnitRepository.save(unit));
    }

    @Transactional(readOnly = true)
    public List<RentalUnitResponse> getPropertyUnits(UUID propertyId) {
        return rentalUnitRepository.findByPropertyId(propertyId)
                .stream()
                .map(RentalUnitResponse::from)
                .toList();
    }
}
