--liquibase formatted sql

--changeset furkan:006-create-contract-signatures
CREATE TABLE contract_schema.contract_signatures (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contract_schema.contracts(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES contract_schema.contract_files(id),
    signer_name VARCHAR(255) NOT NULL,
    signer_email VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(32) NOT NULL,
    external_ref VARCHAR(255),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    signed_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_contract_signatures_contract ON contract_schema.contract_signatures(contract_id);
CREATE INDEX idx_contract_signatures_status   ON contract_schema.contract_signatures(status);
CREATE INDEX idx_contract_signatures_token    ON contract_schema.contract_signatures(token);
