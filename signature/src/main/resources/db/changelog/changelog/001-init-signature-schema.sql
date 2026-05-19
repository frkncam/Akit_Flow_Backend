--liquibase formatted sql

--changeset furkan:001-create-signatures
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
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    token VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(32) NOT NULL,
    external_ref VARCHAR(255),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    signed_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    batch_id UUID NOT NULL,
    signed_file_storage_key VARCHAR(512),
    signature_metadata JSONB,
    CONSTRAINT chk_signatures_status
        CHECK (status IN ('PENDING','SIGNED','REJECTED','CANCELLED','EXPIRED'))
);

CREATE INDEX idx_signatures_token ON signature_schema.signatures(token);
CREATE INDEX idx_signatures_contract ON signature_schema.signatures(contract_id);
CREATE INDEX idx_signatures_batch ON signature_schema.signatures(contract_id, batch_id);
CREATE INDEX idx_signatures_org ON signature_schema.signatures(organization_id);

--changeset furkan:002-create-org-certificates
CREATE TABLE signature_schema.organization_certificates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE,
    certificate_pem TEXT NOT NULL,
    private_key_pem TEXT NOT NULL,
    serial_number VARCHAR(64) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
