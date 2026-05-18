--liquibase formatted sql

--changeset furkan:004-add-expiring-notification-fields
ALTER TABLE contract_schema.contracts
    ADD COLUMN notified_30d_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN notified_7d_at  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN notified_1d_at  TIMESTAMP WITH TIME ZONE;
