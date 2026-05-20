--liquibase formatted sql

--changeset furkan:011-drop-legacy-contract-tables
-- These tables were moved to template_schema and signature_schema in Plan 02.
-- They are no longer used by the contract service.

DROP TABLE IF EXISTS contract_schema.organization_certificates CASCADE;
DROP TABLE IF EXISTS contract_schema.contract_signatures CASCADE;
DROP TABLE IF EXISTS contract_schema.contract_templates CASCADE;
