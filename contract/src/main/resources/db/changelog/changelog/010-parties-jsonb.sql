--liquibase formatted sql

--changeset furkan:010-add-parties-jsonb
ALTER TABLE contract_schema.contracts ADD COLUMN parties_jsonb JSONB;

--changeset furkan:010-migrate-parties-data
UPDATE contract_schema.contracts SET parties_jsonb = parties::jsonb WHERE parties IS NOT NULL AND parties_jsonb IS NULL;

--changeset furkan:010-make-parties-jsonb-not-null
ALTER TABLE contract_schema.contracts ALTER COLUMN parties_jsonb SET NOT NULL;

--changeset furkan:010-drop-old-parties
ALTER TABLE contract_schema.contracts DROP COLUMN parties;

--changeset furkan:010-rename-jsonb-to-parties
ALTER TABLE contract_schema.contracts RENAME COLUMN parties_jsonb TO parties;

--changeset furkan:010-gin-index-on-parties
CREATE INDEX idx_contracts_parties_gin ON contract_schema.contracts USING GIN (parties);
