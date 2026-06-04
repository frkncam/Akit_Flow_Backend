--liquibase formatted sql

-- ============================================================
-- ai_schema: AI servisi icin veritabani semasi
-- ============================================================

--changeset akitflow:001-init-ai-schema
CREATE SCHEMA IF NOT EXISTS ai_schema;

-- 1. Prompt versiyonlari tablosu
CREATE TABLE ai_schema.prompt_versions (
    id              BIGSERIAL PRIMARY KEY,
    prompt_key      VARCHAR(64)  NOT NULL,
    version         INTEGER      NOT NULL DEFAULT 1,
    model           VARCHAR(64),
    system_prompt   TEXT         NOT NULL,
    user_template   TEXT         NOT NULL,
    description     VARCHAR(512),
    is_active       BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_prompt_key_version UNIQUE (prompt_key, version)
);

-- 2. Uretim loglari tablosu (maliyet takibi ve metrikler icin)
CREATE TABLE ai_schema.generation_logs (
    id                  BIGSERIAL PRIMARY KEY,
    organization_id     BIGINT       NOT NULL,
    user_id             BIGINT       NOT NULL,
    prompt_key          VARCHAR(64)  NOT NULL,
    prompt_version      INTEGER      NOT NULL,
    model               VARCHAR(64)  NOT NULL,
    prompt_tokens       INTEGER      NOT NULL DEFAULT 0,
    completion_tokens   INTEGER      NOT NULL DEFAULT 0,
    total_tokens        INTEGER      NOT NULL DEFAULT 0,
    duration_ms         BIGINT       NOT NULL,
    success             BOOLEAN      NOT NULL,
    validation_errors   TEXT,
    error_message       TEXT,
    user_prompt_hash    VARCHAR(64),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gen_logs_org_time    ON ai_schema.generation_logs(organization_id, created_at DESC);
CREATE INDEX idx_gen_logs_success     ON ai_schema.generation_logs(success);
CREATE INDEX idx_gen_logs_prompt_hash ON ai_schema.generation_logs(user_prompt_hash);
