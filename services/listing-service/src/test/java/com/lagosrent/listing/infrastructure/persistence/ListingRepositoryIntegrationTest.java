package com.lagosrent.listing.infrastructure.persistence;

import com.lagosrent.listing.TestcontainersConfiguration;
import com.lagosrent.listing.domain.model.Listing;
import com.lagosrent.listing.domain.model.Property;
import com.lagosrent.listing.domain.model.RentalUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static com.lagosrent.listing.domain.model.ListingStatus.PUBLISHED;
import static com.lagosrent.listing.domain.model.ListingStatus.RESERVED;
import static com.lagosrent.listing.domain.model.UnitType.APARTMENT;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("Listing Repository Atomic Concurrency & PostgreSQL Integration Test")
public class ListingRepositoryIntegrationTest {
    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RentalUnitRepository rentalUnitRepository;

    private Listing listing;
    private final String landlordId = "818746e8-5729-4aa5-91fc-a99199dbd2aa";

    @BeforeEach
    void setUp() {
        // Save upstream Property & RentalUnit hierarchy
        Property property = propertyRepository.save(new Property(
                landlordId, "Eko Atlantic Towers", "Victoria Island", "Eko Atlantic",
                "Avenue 3, Plot 10", "Near Ocean View"
        ));

        RentalUnit unit = rentalUnitRepository.save(new RentalUnit(
                property, "Tower-B-12", APARTMENT, 3, 3, 4
        ));

        listing = listingRepository.save(new Listing(
                unit, landlordId, "Ultra-Luxury 3-Bed Penthouse", "Ocean view",
                new BigDecimal("5000000.00"), now().plusDays(14)
        ));
    }

    @Test
    @DisplayName("Atomically reserving a PUBLISHED listing succeeds and updates status to RESERVED")
    void atomicallyReservePublishedListingSucceeds() {
        // Given: Listing is in PUBLISHED state
        listing.setStatus(PUBLISHED);
        listingRepository.saveAndFlush(listing);

        // When: Attempting atomic reservation
        int updatedRows = listingRepository.atomicallyReserveListing(listing.getId(), Instant.now());

        // Then: Exactly 1 row was updated
        assertThat(updatedRows).isEqualTo(1);

        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RESERVED);
    }

    @Test
    @DisplayName("Atomically reserving an already RESERVED listing returns 0 (prevents double booking)")
    void atomicallyReserveAlreadyReservedListing() {
        // Given: Listing is already RESERVED by another tenant
        listing.setStatus(RESERVED);
        listingRepository.saveAndFlush(listing);

        // When: A second reservation request arrives concurrently
        int updatedRows = listingRepository.atomicallyReserveListing(listing.getId(), Instant.now());

        // Then: 0 rows are updated (concurrency race prevented atomically in PostgreSQL)
        assertThat(updatedRows).isEqualTo(0);

        Listing untouched = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(untouched.getStatus()).isEqualTo(RESERVED);
    }

    @Test
    @DisplayName("Compensating action: atomically release a held listing restored status to PUBLISHED")
    void atomicallyReleaseListingRestoresPublishedStatus() {
        // Given: A listing currently held under RESERVED status
        listing.setStatus(RESERVED);
        listingRepository.saveAndFlush(listing);

        // When: Reservation expires or is cancelled
        int updatedRows = listingRepository.atomicallyReleaseListing(listing.getId(), Instant.now());

        // Then: Exactly 1 row restored to PUBLISHED
        assertThat(updatedRows).isEqualTo(1);

        Listing released = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(PUBLISHED);
    }
}
