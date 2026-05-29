--liquibase formatted sql

--changeset akitflow:001-init-auth-schema
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.organizations (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE auth_schema.users (
    id               BIGSERIAL    PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES auth_schema.organizations(id),
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    role             VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    status           VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE auth_schema.refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES auth_schema.users(id),
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE auth_schema.invite_tokens (
    id               BIGSERIAL    PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES auth_schema.organizations(id),
    invited_by       BIGINT       NOT NULL REFERENCES auth_schema.users(id),
    email            VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    token_hash       VARCHAR(255) NOT NULL,
    expires_at       TIMESTAMPTZ  NOT NULL,
    used_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE auth_schema.failed_events (
    id               BIGSERIAL     PRIMARY KEY,
    event_json       TEXT          NOT NULL,
    exchange         VARCHAR(128)  NOT NULL,
    routing_key      VARCHAR(128)  NOT NULL,
    event_type       VARCHAR(128)  NOT NULL,
    last_error       VARCHAR(1024),
    terminal_failure BOOLEAN       NOT NULL DEFAULT FALSE,
    retry_count      INTEGER       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_retry_at    TIMESTAMPTZ
);

CREATE INDEX idx_users_org        ON auth_schema.users(organization_id);
CREATE INDEX idx_users_email      ON auth_schema.users(email);
CREATE INDEX idx_refresh_user     ON auth_schema.refresh_tokens(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_invite_email_org ON auth_schema.invite_tokens(email, organization_id) WHERE used_at IS NULL;
