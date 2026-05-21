--liquibase formatted sql

--changeset furkan:004-add-approval-step-version
ALTER TABLE workflow_schema.approval_steps ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
