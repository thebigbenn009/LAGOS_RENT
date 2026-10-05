-- ==============================================================================
-- Lagos Rent Platform — Rental Service Initial Schema (V1)
-- ==============================================================================

-- 1. Inspection Requests Table
CREATE TABLE IF NOT EXISTS inspection_requests (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    tenant_keycloak_id VARCHAR(255) NOT NULL,
    landlord_keycloak_id VARCHAR(255) NOT NULL,
    proposed_datetime TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_datetime TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspections_tenant ON inspection_requests(tenant_keycloak_id);
CREATE INDEX idx_inspections_landlord ON inspection_requests(landlord_keycloak_id);
CREATE INDEX idx_inspections_listing ON inspection_requests(listing_id);

-- 2. Rental Applications Table
CREATE TABLE IF NOT EXISTS rental_applications (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    tenant_keycloak_id VARCHAR(255) NOT NULL,
    landlord_keycloak_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    employment_status VARCHAR(100),
    annual_income NUMERIC(15, 2),
    previous_landlord_contact VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_applications_tenant ON rental_applications(tenant_keycloak_id);
CREATE INDEX idx_applications_landlord ON rental_applications(landlord_keycloak_id);
CREATE INDEX idx_applications_listing ON rental_applications(listing_id);

-- 3. Immutable Price Snapshots Table (ADR-002, Implementation Guide Phase 15)
CREATE TABLE IF NOT EXISTS price_snapshots (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES rental_applications(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL,
    listing_version BIGINT NOT NULL,
    rent_amount NUMERIC(15, 2) NOT NULL,
    caution_deposit NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    service_charge NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    legal_fee NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_move_in_amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NGN',
    fee_breakdown JSONB NOT NULL,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snapshot_application UNIQUE (application_id)
);

CREATE INDEX idx_snapshots_listing ON price_snapshots(listing_id);

-- 4. Application Decisions Table
CREATE TABLE IF NOT EXISTS application_decisions (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES rental_applications(id) ON DELETE CASCADE,
    landlord_keycloak_id VARCHAR(255) NOT NULL,
    decision VARCHAR(50) NOT NULL,
    reason TEXT,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 5. Reservations Table (Rental Service's Half of the Reservation)
CREATE TABLE IF NOT EXISTS reservations (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES rental_applications(id) ON DELETE RESTRICT,
    listing_id UUID NOT NULL,
    reservation_reference VARCHAR(255) NOT NULL UNIQUE,
    hold_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_reference ON reservations(reservation_reference);

-- 6. Rental Sagas Table (Orchestrator State - ADR-009)
CREATE TABLE IF NOT EXISTS rental_sagas (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES rental_applications(id) ON DELETE RESTRICT,
    listing_id UUID NOT NULL,
    reservation_reference VARCHAR(255),
    current_step VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'STARTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saga_application UNIQUE (application_id)
);

-- 7. Transactional Outbox Table (ADR-007)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rental_outbox_unpublished ON outbox_events(occurred_at) WHERE published_at IS NULL;

-- 8. Idempotent Inbox Table (ADR-008)
CREATE TABLE IF NOT EXISTS processed_messages (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);

CREATE INDEX idx_rental_inbox_processed_at ON processed_messages(processed_at);