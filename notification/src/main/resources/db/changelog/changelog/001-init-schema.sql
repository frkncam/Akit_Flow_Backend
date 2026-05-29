--liquibase formatted sql

--changeset akitflow:001-init-notification-schema
CREATE SCHEMA IF NOT EXISTS notification_schema;

CREATE TABLE notification_schema.processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_processed_event_event_id UNIQUE (event_id)
);

CREATE TABLE notification_schema.email_log (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID,
    email_type VARCHAR(32) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    error_message TEXT,
    attempt_count INT NOT NULL DEFAULT 1,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_schema.failed_events (
    id BIGSERIAL PRIMARY KEY,
    event_json TEXT NOT NULL,
    exchange VARCHAR(128) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    last_error VARCHAR(1024),
    terminal_failure BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_retry_at TIMESTAMPTZ
);

CREATE INDEX idx_processed_event_event_id ON notification_schema.processed_event(event_id);
CREATE INDEX idx_email_log_event ON notification_schema.email_log(event_id);
CREATE INDEX idx_email_log_recipient ON notification_schema.email_log(recipient);
CREATE INDEX idx_email_log_status ON notification_schema.email_log(status);
CREATE INDEX idx_email_log_recipient_status ON notification_schema.email_log(recipient, status, created_at DESC);
CREATE INDEX idx_failed_events_terminal ON notification_schema.failed_events(terminal_failure, created_at);
