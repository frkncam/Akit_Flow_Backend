--liquibase formatted sql

--changeset akitflow:001-init-audit-schema
CREATE SCHEMA IF NOT EXISTS audit_schema;

CREATE TABLE audit_schema.audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT,
    organization_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_audit_events_event_id UNIQUE (event_id),
    CONSTRAINT chk_audit_events_aggregate_type CHECK (aggregate_type IN ('CONTRACT', 'USER', 'WORKFLOW', 'SIGNATURE'))
);

CREATE TABLE audit_schema.failed_events (
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

CREATE INDEX idx_audit_events_org_aggregate ON audit_schema.audit_events(organization_id, aggregate_type, aggregate_id);
CREATE INDEX idx_audit_events_recorded_at ON audit_schema.audit_events(recorded_at DESC);
CREATE INDEX idx_audit_events_org_type ON audit_schema.audit_events(organization_id, aggregate_type);
CREATE INDEX idx_audit_events_org_type_recorded ON audit_schema.audit_events(organization_id, aggregate_type, recorded_at DESC);
CREATE INDEX idx_failed_events_terminal ON audit_schema.failed_events(terminal_failure, created_at);

--changeset akitflow:002-audit-append-only-trigger endDelimiter:;;
CREATE OR REPLACE FUNCTION audit_schema.reject_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_events table is append-only. UPDATE and DELETE are not allowed.';
END;
$$ LANGUAGE plpgsql;;

CREATE TRIGGER trg_audit_events_no_modify
    BEFORE UPDATE OR DELETE ON audit_schema.audit_events
    FOR EACH ROW
    EXECUTE FUNCTION audit_schema.reject_audit_modification();;
