-- Phase 16: Payment Service Schema Initialization
-- Database: payment_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Payables Table (Financial obligations created upon application acceptance)
CREATE TABLE IF NOT EXISTS payables (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    tenant_keycloak_id VARCHAR(64) NOT NULL,
    landlord_keycloak_id VARCHAR(64) NOT NULL,
    reservation_reference VARCHAR(64) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NGN',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    due_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payable_application UNIQUE (application_id)
);

CREATE INDEX idx_payables_tenant ON payables(tenant_keycloak_id);
CREATE INDEX idx_payables_landlord ON payables(landlord_keycloak_id);
CREATE INDEX idx_payables_reservation ON payables(reservation_reference);

-- 2. Payment Attempts Table (Tracks individual checkout sessions)
CREATE TABLE IF NOT EXISTS payment_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payable_id UUID NOT NULL REFERENCES payables(id) ON DELETE RESTRICT,
    internal_reference VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    provider_name VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(128),
    authorization_url TEXT,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NGN',
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attempts_payable ON payment_attempts(payable_id);
CREATE INDEX idx_attempts_reference ON payment_attempts(internal_reference);

-- 3. Payment Transactions Table (Immutable audit ledger of settled funds)
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payable_id UUID NOT NULL REFERENCES payables(id) ON DELETE RESTRICT,
    attempt_id UUID REFERENCES payment_attempts(id) ON DELETE RESTRICT,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    provider_reference VARCHAR(128) NOT NULL UNIQUE,
    provider_name VARCHAR(50) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NGN',
    channel VARCHAR(50),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_payable ON payment_transactions(payable_id);

-- 4. Provider Webhooks Table (Idempotent audit log of all raw webhook payloads)
CREATE TABLE IF NOT EXISTS provider_webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_name VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    provider_reference VARCHAR(128),
    payload JSONB NOT NULL,
    signature_header TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    processing_error TEXT,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_webhook_provider_ref UNIQUE (provider_name, event_type, provider_reference)
);

CREATE INDEX idx_webhooks_ref ON provider_webhooks(provider_reference);

-- 5. Receipts Table (Official financial receipt issued after confirmed settlement)
CREATE TABLE IF NOT EXISTS receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES payment_transactions(id) ON DELETE RESTRICT,
    payable_id UUID NOT NULL REFERENCES payables(id) ON DELETE RESTRICT,
    receipt_number VARCHAR(64) NOT NULL UNIQUE,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NGN',
    issued_to_tenant_keycloak_id VARCHAR(64) NOT NULL,
    issued_for_landlord_keycloak_id VARCHAR(64) NOT NULL,
    receipt_data JSONB NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_receipts_payable ON receipts(payable_id);
CREATE INDEX idx_receipts_tenant ON receipts(issued_to_tenant_keycloak_id);

-- 6. Outbox Events Table (Transactional Outbox for domain events)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(100),
    causation_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);