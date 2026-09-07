package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.domain.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    List<Property> findByLandlordKeycloakId(String landlordKeycloakId);
}
