--liquibase formatted sql

--changeset furkan:001-create-audit-events
CREATE TABLE audit_schema.audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT,
    organization_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_audit_aggregate_type CHECK (aggregate_type IN ('CONTRACT','USER'))
);
CREATE INDEX idx_audit_org_aggregate ON audit_schema.audit_events(organization_id, aggregate_type, aggregate_id);
CREATE INDEX idx_audit_org_actor    ON audit_schema.audit_events(organization_id, actor_id);
CREATE INDEX idx_audit_recorded_at  ON audit_schema.audit_events(recorded_at DESC);
CREATE INDEX idx_audit_org_type     ON audit_schema.audit_events(organization_id, event_type);
