--liquibase formatted sql

--changeset furkan:001-create-processed-event
CREATE TABLE notification_schema.processed_event (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_event_event_id ON notification_schema.processed_event(event_id);

--changeset furkan:002-create-email-log
CREATE TABLE notification_schema.email_log (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID,
    email_type VARCHAR(32) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    error_message TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_log_recipient ON notification_schema.email_log(recipient);
CREATE INDEX idx_email_log_status ON notification_schema.email_log(status);
CREATE INDEX idx_email_log_event_id ON notification_schema.email_log(event_id);
