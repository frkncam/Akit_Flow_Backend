--liquibase formatted sql

--changeset furkan:008-e-signature-signing
ALTER TABLE contract_schema.contract_signatures
    ADD COLUMN signed_file_storage_key VARCHAR(512);

ALTER TABLE contract_schema.contract_signatures
    ADD COLUMN signature_metadata JSONB;

--changeset furkan:009-organization-certificates
CREATE TABLE contract_schema.organization_certificates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE,
    certificate_pem TEXT NOT NULL,
    private_key_pem TEXT NOT NULL,
    serial_number VARCHAR(64) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
