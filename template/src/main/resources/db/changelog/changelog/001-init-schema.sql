--liquibase formatted sql

--changeset akitflow:001-init-template-schema
CREATE SCHEMA IF NOT EXISTS template_schema;

CREATE TABLE template_schema.templates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(32),
    body_html TEXT NOT NULL,
    variables JSONB NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_templates_category CHECK (category IS NULL OR category IN ('NDA', 'EMPLOYMENT', 'SERVICE', 'RENTAL', 'OTHER'))
);

CREATE TABLE template_schema.failed_events (
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

CREATE INDEX idx_templates_organization ON template_schema.templates(organization_id);
CREATE INDEX idx_templates_org_category ON template_schema.templates(organization_id, category);
CREATE INDEX idx_failed_events_terminal ON template_schema.failed_events(terminal_failure, created_at);
