--liquibase formatted sql

--changeset akitflow:002-otp-audit-tsa
ALTER TABLE signature_schema.signatures ADD COLUMN document_hash      VARCHAR(64);
ALTER TABLE signature_schema.signatures ADD COLUMN otp_hash           VARCHAR(128);
ALTER TABLE signature_schema.signatures ADD COLUMN otp_expires_at     TIMESTAMPTZ;
ALTER TABLE signature_schema.signatures ADD COLUMN otp_attempts       INT DEFAULT 0;
ALTER TABLE signature_schema.signatures ADD COLUMN otp_verified_at    TIMESTAMPTZ;
ALTER TABLE signature_schema.signatures ADD COLUMN signer_ip          VARCHAR(64);
ALTER TABLE signature_schema.signatures ADD COLUMN signer_user_agent  VARCHAR(512);
ALTER TABLE signature_schema.signatures ADD COLUMN consent_accepted_at TIMESTAMPTZ;
ALTER TABLE signature_schema.signatures ADD COLUMN tsa_time           TIMESTAMPTZ;
