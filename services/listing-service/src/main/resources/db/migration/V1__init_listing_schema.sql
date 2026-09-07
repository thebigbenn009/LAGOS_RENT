-- ==============================================================================
-- Lagos Rent Platform — Listing Service Initial Schema (V1)
-- ==============================================================================

-- 1. Properties Table (Physical building location)
CREATE TABLE IF NOT EXISTS properties (
    id UUID PRIMARY KEY,
    landlord_keycloak_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    state VARCHAR(100) NOT NULL DEFAULT 'Lagos',
    lga VARCHAR(100) NOT NULL,
    area VARCHAR(100) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    landmark VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_properties_landlord ON properties(landlord_keycloak_id);
CREATE INDEX idx_properties_location ON properties(state, lga, area);

-- 2. Rental Units Table (Specific rentable space within a property)
CREATE TABLE IF NOT EXISTS rental_units (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    unit_number VARCHAR(50) NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    bedrooms INT NOT NULL DEFAULT 1,
    bathrooms INT NOT NULL DEFAULT 1,
    toilets INT NOT NULL DEFAULT 1,
    size_sq_meters NUMERIC(8, 2),
    floor_number INT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_property_unit_number UNIQUE (property_id, unit_number)
);

CREATE INDEX idx_rental_units_property ON rental_units(property_id);

-- 3. Listings Table (The market offer for a unit at a point in time)
CREATE TABLE IF NOT EXISTS listings (
    id UUID PRIMARY KEY,
    rental_unit_id UUID NOT NULL REFERENCES rental_units(id) ON DELETE RESTRICT,
    landlord_keycloak_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    rent_amount NUMERIC(15, 2) NOT NULL,
    rent_period VARCHAR(50) NOT NULL DEFAULT 'ANNUALLY',
    move_in_total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    available_from DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_unit ON listings(rental_unit_id);
CREATE INDEX idx_listings_landlord ON listings(landlord_keycloak_id);
CREATE INDEX idx_listings_status ON listings(status);

-- 4. Listing Fees Table (Itemized transparent fee breakdown)
CREATE TABLE IF NOT EXISTS listing_fees (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    fee_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    recipient VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL DEFAULT 'ONE_OFF',
    is_refundable BOOLEAN NOT NULL DEFAULT FALSE,
    explanation TEXT NOT NULL,
    approved_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_fees_listing ON listing_fees(listing_id);

-- 5. Listing Media Table (Photos, floor plans, video walkthroughs)
CREATE TABLE IF NOT EXISTS listing_media (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    media_type VARCHAR(50) NOT NULL DEFAULT 'IMAGE',
    s3_key VARCHAR(500) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    caption VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_media_listing ON listing_media(listing_id);

-- 6. Listing Reviews Table (Admin review history)
CREATE TABLE IF NOT EXISTS listing_reviews (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    admin_keycloak_id VARCHAR(255) NOT NULL,
    decision VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    listing_version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_reviews_listing ON listing_reviews(listing_id);

-- 7. Listing Reservations Table (Temporary holds during application workflows)
CREATE TABLE IF NOT EXISTS listing_reservations (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    reservation_reference VARCHAR(255) NOT NULL UNIQUE,
    held_until TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_reservations_listing ON listing_reservations(listing_id);

-- 8. Admin Audit Records Table (Immutable admin action log)
CREATE TABLE IF NOT EXISTS admin_audit_records (
    id UUID PRIMARY KEY,
    admin_keycloak_id VARCHAR(255) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    previous_state TEXT,
    new_state TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_audit_records_entity ON admin_audit_records(entity_type, entity_id);