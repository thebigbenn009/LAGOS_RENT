-- ==============================================================================
-- Lagos Rent Platform — Search Service Projection & Inbox Schema (V1)
-- ==============================================================================

-- 1. Denormalized Search Listings Projection Table
CREATE TABLE IF NOT EXISTS search_listings (
    listing_id UUID PRIMARY KEY,
    listing_version BIGINT NOT NULL,
    landlord_keycloak_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    rent_amount NUMERIC(15, 2) NOT NULL,
    rent_period VARCHAR(50) NOT NULL DEFAULT 'ANNUALLY',
    move_in_total NUMERIC(15, 2) NOT NULL,
    available_from DATE,
    state VARCHAR(100) NOT NULL DEFAULT 'Lagos',
    lga VARCHAR(100) NOT NULL,
    area VARCHAR(100) NOT NULL,
    landmark VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    unit_type VARCHAR(50) NOT NULL,
    bedrooms INT NOT NULL DEFAULT 1,
    bathrooms INT NOT NULL DEFAULT 1,
    toilets INT NOT NULL DEFAULT 1,
    size_sq_meters NUMERIC(8, 2),
    floor_number INT,
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED',
    primary_image_url VARCHAR(500),
    fee_summary JSONB,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Compound and specific indexes for multi-filter fast searching
CREATE INDEX idx_search_location ON search_listings(state, lga, area);
CREATE INDEX idx_search_rent ON search_listings(rent_amount);
CREATE INDEX idx_search_bedrooms ON search_listings(bedrooms);
CREATE INDEX idx_search_unit_type ON search_listings(unit_type);
CREATE INDEX idx_search_status ON search_listings(status);
CREATE INDEX idx_search_published_at ON search_listings(published_at DESC);

-- 2. Idempotent Inbox Table (ADR-008)
CREATE TABLE IF NOT EXISTS processed_messages (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);

CREATE INDEX idx_processed_messages_at ON processed_messages(processed_at);