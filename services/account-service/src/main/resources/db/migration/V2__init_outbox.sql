-- Phase 9/12 Retrofit: Account Service Transactional Outbox Table
-- Database: account_db

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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

-- Index for high-throughput FIFO polling of unpublished events
CREATE INDEX idx_account_outbox_unpublished ON outbox_events(occurred_at) WHERE published_at IS NULL;
CREATE INDEX idx_account_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);