--liquibase formatted sql

--changeset akitflow:001-init-workflow-schema
CREATE SCHEMA IF NOT EXISTS workflow_schema;

CREATE TABLE workflow_schema.workflow_instances (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    current_state VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_workflow_instances_contract UNIQUE (contract_id),
    CONSTRAINT chk_workflow_instances_state CHECK (current_state IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE TABLE workflow_schema.approval_steps (
    id BIGSERIAL PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL,
    order_index INT NOT NULL,
    approver_user_id BIGINT NOT NULL,
    approver_name VARCHAR(255),
    approver_email VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    comment TEXT,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_approval_steps_order UNIQUE (workflow_instance_id, order_index),
    CONSTRAINT chk_approval_steps_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_approval_steps_workflow FOREIGN KEY (workflow_instance_id) REFERENCES workflow_schema.workflow_instances(id)
);

CREATE TABLE workflow_schema.workflow_transitions (
    id BIGSERIAL PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL,
    from_state VARCHAR(32),
    to_state VARCHAR(32) NOT NULL,
    event VARCHAR(32) NOT NULL,
    triggered_by BIGINT,
    comment TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_workflow_transitions_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_schema.workflow_instances(id)
);

CREATE TABLE workflow_schema.failed_events (
    id BIGSERIAL PRIMARY KEY,
    event_json TEXT NOT NULL,
    exchange VARCHAR(128) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    last_error VARCHAR(1024),
    terminal_failure BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_retry_at TIMESTAMPTZ
);

CREATE INDEX idx_workflow_instances_org ON workflow_schema.workflow_instances(organization_id);
CREATE INDEX idx_workflow_instances_org_state ON workflow_schema.workflow_instances(organization_id, current_state);
CREATE INDEX idx_approval_steps_workflow ON workflow_schema.approval_steps(workflow_instance_id);
CREATE INDEX idx_approval_steps_approver ON workflow_schema.approval_steps(approver_user_id);
CREATE INDEX idx_workflow_transitions_instance ON workflow_schema.workflow_transitions(workflow_instance_id);
CREATE INDEX idx_workflow_transitions_occurred ON workflow_schema.workflow_transitions(workflow_instance_id, occurred_at DESC);
CREATE INDEX idx_failed_events_terminal ON workflow_schema.failed_events(terminal_failure, created_at);
