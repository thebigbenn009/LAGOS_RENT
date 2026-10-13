-- Phase 17: Tenancy Activation Table
CREATE TABLE IF NOT EXISTS tenancies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES rental_applications(id) ON DELETE RESTRICT,
    listing_id UUID NOT NULL,
    tenant_keycloak_id VARCHAR(64) NOT NULL,
    landlord_keycloak_id VARCHAR(64) NOT NULL,
    annual_rent NUMERIC(15, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenancy_application UNIQUE (application_id)
);

CREATE INDEX idx_tenancies_tenant ON tenancies(tenant_keycloak_id);
CREATE INDEX idx_tenancies_listing ON tenancies(listing_id);