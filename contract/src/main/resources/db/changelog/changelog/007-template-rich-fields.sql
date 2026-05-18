--liquibase formatted sql

--changeset furkan:008-template-rich-fields
ALTER TABLE contract_schema.contract_templates
    RENAME COLUMN content TO body_html;

ALTER TABLE contract_schema.contract_templates
    ADD COLUMN category VARCHAR(32),
    ADD COLUMN variables JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE contract_schema.contract_templates
    ADD CONSTRAINT chk_contract_templates_category
    CHECK (category IS NULL OR category IN ('NDA','EMPLOYMENT','SERVICE','RENTAL','OTHER'));

CREATE INDEX idx_contract_templates_category
    ON contract_schema.contract_templates(organization_id, category);
