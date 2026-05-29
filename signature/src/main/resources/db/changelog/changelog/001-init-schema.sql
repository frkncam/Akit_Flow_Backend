--liquibase formatted sql

--changeset akitflow:001-init-signature-schema
CREATE SCHEMA IF NOT EXISTS signature_schema;

CREATE TABLE signature_schema.signatures (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    contract_title VARCHAR(255) NOT NULL,
    file_id BIGINT NOT NULL,
    file_storage_key VARCHAR(512) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    organization_id BIGINT NOT NULL,
    signer_name VARCHAR(255) NOT NULL,
    signer_email VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    token VARCHAR(64) NOT NULL,
    provider_name VARCHAR(32) NOT NULL,
    external_ref VARCHAR(255),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    signed_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    batch_id UUID NOT NULL,
    signed_file_storage_key VARCHAR(512),
    signature_metadata JSONB,
    CONSTRAINT uq_signatures_token UNIQUE (token),
    CONSTRAINT chk_signatures_status CHECK (status IN ('PENDING', 'SIGNED', 'REJECTED', 'CANCELLED', 'EXPIRED'))
);

CREATE TABLE signature_schema.organization_certificates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    certificate_pem TEXT NOT NULL,
    private_key_pem TEXT NOT NULL,
    serial_number VARCHAR(64) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_org_certs_organization UNIQUE (organization_id)
);

CREATE TABLE signature_schema.failed_events (
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

CREATE INDEX idx_signatures_token ON signature_schema.signatures(token);
CREATE INDEX idx_signatures_contract ON signature_schema.signatures(contract_id);
CREATE INDEX idx_signatures_batch ON signature_schema.signatures(batch_id);
CREATE INDEX idx_signatures_organization ON signature_schema.signatures(organization_id);
CREATE INDEX idx_signatures_org_status ON signature_schema.signatures(organization_id, status);
CREATE INDEX idx_failed_events_terminal ON signature_schema.failed_events(terminal_failure, created_at);
