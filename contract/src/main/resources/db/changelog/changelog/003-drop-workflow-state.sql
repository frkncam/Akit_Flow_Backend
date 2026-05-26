--liquibase formatted sql

--changeset furkan:005-drop-workflow-state
ALTER TABLE contract_schema.contracts DROP COLUMN IF EXISTS workflow_state;
