--liquibase formatted sql

--changeset furkan:001-create-contracts
CREATE TABLE contract_schema.contracts (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    contract_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    parties TEXT NOT NULL DEFAULT '[]',
    start_date DATE,
    end_date DATE,
    signed_at TIMESTAMP WITH TIME ZONE,
    terminated_at TIMESTAMP WITH TIME ZONE,
    value NUMERIC(19,2),
    currency VARCHAR(3),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contracts_org ON contract_schema.contracts(organization_id);
CREATE INDEX idx_contracts_status ON contract_schema.contracts(status);
CREATE INDEX idx_contracts_end_date ON contract_schema.contracts(end_date);

--changeset furkan:002-create-contract-files
CREATE TABLE contract_schema.contract_files (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contract_schema.contracts(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    version INTEGER NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contract_files_contract ON contract_schema.contract_files(contract_id);
