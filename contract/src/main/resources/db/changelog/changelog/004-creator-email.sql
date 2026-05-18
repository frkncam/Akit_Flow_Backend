--liquibase formatted sql

--changeset furkan:005-add-creator-email
ALTER TABLE contract_schema.contracts ADD COLUMN creator_email VARCHAR(255);
