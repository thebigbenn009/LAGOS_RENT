-- ==============================================================================
-- Lagos Rent Platform — Account Service Initial Schema (V1)
-- ==============================================================================

-- 1. Accounts Table (Primary entity linked to Keycloak via keycloak_user_id)
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_keycloak_id ON accounts(keycloak_user_id);
CREATE INDEX idx_accounts_email ON accounts(email);

-- 2. Tenant Profiles Table (Specific preferences and details for tenants)
CREATE TABLE IF NOT EXISTS tenant_profiles (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    occupation VARCHAR(255),
    annual_budget NUMERIC(15, 2),
    move_in_date DATE,
    preferred_lga VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Landlord Profiles Table (Specific business details and verification status)
CREATE TABLE IF NOT EXISTS landlord_profiles (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'UNVERIFIED',
    business_name VARCHAR(255),
    bank_account_name VARCHAR(255),
    bank_account_number VARCHAR(50),
    bank_name VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_landlord_profiles_status ON landlord_profiles(verification_status);

-- 4. Verification Cases Table (KYC / property authority documents in AWS S3)
CREATE TABLE IF NOT EXISTS verification_cases (
    id UUID PRIMARY KEY,
    landlord_profile_id UUID NOT NULL REFERENCES landlord_profiles(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_s3_key VARCHAR(500) NOT NULL,
    document_filename VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    admin_notes TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_verification_cases_landlord ON verification_cases(landlord_profile_id);
CREATE INDEX idx_verification_cases_status ON verification_cases(status);

-- 5. Saved Listings Table (Tenants saving listing IDs for later)
CREATE TABLE IF NOT EXISTS saved_listings (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_account_saved_listing UNIQUE (account_id, listing_id)
);

CREATE INDEX idx_saved_listings_account ON saved_listings(account_id);

-- 6. Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 7. Admin Audit Records Table (Immutable log of all administrative actions)
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

CREATE INDEX idx_admin_audit_entity ON admin_audit_records(entity_type, entity_id);
CREATE INDEX idx_admin_audit_admin ON admin_audit_records(admin_keycloak_id);