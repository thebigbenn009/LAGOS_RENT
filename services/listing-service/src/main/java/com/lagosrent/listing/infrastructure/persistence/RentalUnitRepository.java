package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.RentalUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RentalUnitRepository extends JpaRepository<RentalUnit, UUID> {
    List<RentalUnit> findByPropertyId(UUID propertyId);

    Optional<RentalUnit> findByPropertyIdAndUnitNumber(UUID propertyId, String unitNumber);
}
