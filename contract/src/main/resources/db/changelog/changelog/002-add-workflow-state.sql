--liquibase formatted sql

--changeset furkan:004-add-workflow-state
ALTER TABLE contract_schema.contracts ADD COLUMN IF NOT EXISTS workflow_state VARCHAR(32);
