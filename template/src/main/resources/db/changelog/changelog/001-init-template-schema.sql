--liquibase formatted sql

--changeset furkan:001-create-templates
CREATE TABLE template_schema.templates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(32),
    body_html TEXT NOT NULL,
    variables JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_templates_category
        CHECK (category IS NULL OR category IN ('NDA','EMPLOYMENT','SERVICE','RENTAL','OTHER'))
);

CREATE INDEX idx_templates_org ON template_schema.templates(organization_id);
CREATE INDEX idx_templates_org_category ON template_schema.templates(organization_id, category);
