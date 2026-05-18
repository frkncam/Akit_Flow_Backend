--liquibase formatted sql

--changeset akitflow:001 labels:init comment:auth_schema tabloları

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

CREATE INDEX idx_users_org        ON auth_schema.users(organization_id);
CREATE INDEX idx_users_email      ON auth_schema.users(email);
CREATE INDEX idx_refresh_user     ON auth_schema.refresh_tokens(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_invite_email_org ON auth_schema.invite_tokens(email, organization_id) WHERE used_at IS NULL;

--rollback DROP TABLE auth_schema.invite_tokens;
--rollback DROP TABLE auth_schema.refresh_tokens;
--rollback DROP TABLE auth_schema.users;
--rollback DROP TABLE auth_schema.organizations;
