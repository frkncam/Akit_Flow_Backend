--liquibase formatted sql

--changeset akitflow:001-init-contract-schema
CREATE SCHEMA IF NOT EXISTS contract_schema;

CREATE TABLE contract_schema.contracts (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    contract_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    parties JSONB NOT NULL,
    start_date DATE,
    end_date DATE,
    signed_at TIMESTAMPTZ,
    terminated_at TIMESTAMPTZ,
    value NUMERIC(19, 2),
    currency VARCHAR(3),
    created_by BIGINT NOT NULL,
    creator_email VARCHAR(255),
    notified_30d_at TIMESTAMPTZ,
    notified_7d_at TIMESTAMPTZ,
    notified_1d_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE contract_schema.contract_files (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    version INT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_contract_files_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_contract_files_contract FOREIGN KEY (contract_id) REFERENCES contract_schema.contracts(id) ON DELETE CASCADE
);

CREATE TABLE contract_schema.processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE contract_schema.failed_events (
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

CREATE INDEX idx_contracts_organization ON contract_schema.contracts(organization_id);
CREATE INDEX idx_contracts_status ON contract_schema.contracts(status);
CREATE INDEX idx_contracts_end_date ON contract_schema.contracts(end_date);
CREATE INDEX idx_contracts_parties_gin ON contract_schema.contracts USING GIN (parties);
CREATE INDEX idx_contracts_org_status ON contract_schema.contracts(organization_id, status);
CREATE INDEX idx_contract_files_contract ON contract_schema.contract_files(contract_id);
CREATE INDEX idx_failed_events_terminal ON contract_schema.failed_events(terminal_failure, created_at);
