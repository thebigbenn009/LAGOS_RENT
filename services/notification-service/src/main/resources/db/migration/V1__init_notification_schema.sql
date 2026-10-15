-- Phase 18: Notification Service Schema Initialization
-- Database: notification_db

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Notification Templates Table (Versioned, parameter-driven email/SMS copy)
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_code VARCHAR(100) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    is_html BOOLEAN NOT NULL DEFAULT true,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Notification Deliveries Table (Immutable audit ledger of every dispatch attempt)
CREATE TABLE IF NOT EXISTS notification_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    subject TEXT NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    provider_name VARCHAR(50),
    provider_message_id VARCHAR(255),
    error_message TEXT,
    attempt_count INT NOT NULL DEFAULT 1,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_idempotency UNIQUE (event_id, template_code, recipient)
);

CREATE INDEX idx_deliveries_recipient ON notification_deliveries(recipient);
CREATE INDEX idx_deliveries_event ON notification_deliveries(event_id);
CREATE INDEX idx_deliveries_status ON notification_deliveries(status);

-- 3. Notification Preferences Table (User opt-ins/opt-outs)
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_keycloak_id VARCHAR(64) NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    sms_enabled BOOLEAN NOT NULL DEFAULT true,
    marketing_enabled BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_preferences_user ON notification_preferences(user_keycloak_id);

-- Seed Initial System Notification Templates
INSERT INTO notification_templates (template_code, channel, subject_template, body_template, is_html, version)
VALUES
('PAYMENT_RECEIPT', 'EMAIL', 'Payment Receipt Confirmed: {{receiptNumber}} - Lagos Rent',
 '<h2>Payment Confirmed!</h2><p>Dear {{recipientName}},</p><p>We have successfully received your payment of <strong>{{amount}} {{currency}}</strong> for reservation <strong>{{reservationReference}}</strong>.</p><p>Your official receipt number is: <strong>{{receiptNumber}}</strong>.</p><p>Thank you for choosing Lagos Rent.</p>',
 true, 1),

('TENANCY_ACTIVATED', 'EMAIL', 'Congratulations! Your Tenancy is Active - Lagos Rent',
 '<h2>Welcome to Your New Home!</h2><p>Dear {{recipientName}},</p><p>Your tenancy for <strong>{{propertyTitle}}</strong> has been officially confirmed and activated.</p><p>Lease Period: <strong>{{startDate}}</strong> to <strong>{{endDate}}</strong>.</p><p>Your landlord and facility manager have been notified.</p>',
 true, 1),

('RESERVATION_CONFIRMED', 'EMAIL', 'Reservation Confirmed: Next Steps to Complete Payment - Lagos Rent',
 '<h2>Your Application was Accepted!</h2><p>Dear {{recipientName}},</p><p>The landlord has accepted your rental application. Your reservation hold is active under reference: <strong>{{reservationReference}}</strong>.</p><p>Total Move-In Cost: <strong>{{totalAmount}} {{currency}}</strong>.</p><p>Please log in to your dashboard to complete payment before your hold expires on <strong>{{holdDeadline}}</strong>.</p>',
 true, 1),

('LANDLORD_VERIFIED', 'EMAIL', 'Your Landlord Account is Verified - Lagos Rent',
 '<h2>Account Verification Approved!</h2><p>Dear {{recipientName}},</p><p>Congratulations! Your landlord identity and documents have been verified by Lagos Rent administrators. You can now publish rental listings on the platform.</p>',
 true, 1)
ON CONFLICT (template_code) DO NOTHING;