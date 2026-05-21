--liquibase formatted sql

--changeset furkan:001-create-workflow-instances
CREATE TABLE workflow_schema.workflow_instances (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    current_state VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_wf_state CHECK (current_state IN
        ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED'))
);
CREATE INDEX idx_wf_instances_org ON workflow_schema.workflow_instances(organization_id);

--changeset furkan:002-create-approval-steps
CREATE TABLE workflow_schema.approval_steps (
    id BIGSERIAL PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL REFERENCES workflow_schema.workflow_instances(id),
    order_index INT NOT NULL,
    approver_user_id BIGINT NOT NULL,
    approver_name VARCHAR(255),
    approver_email VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    comment TEXT,
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_step_order UNIQUE (workflow_instance_id, order_index),
    CONSTRAINT chk_step_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);
CREATE INDEX idx_steps_instance ON workflow_schema.approval_steps(workflow_instance_id);
CREATE INDEX idx_steps_approver ON workflow_schema.approval_steps(approver_user_id, status);

--changeset furkan:003-create-workflow-transitions
CREATE TABLE workflow_schema.workflow_transitions (
    id BIGSERIAL PRIMARY KEY,
    workflow_instance_id BIGINT NOT NULL REFERENCES workflow_schema.workflow_instances(id),
    from_state VARCHAR(32),
    to_state VARCHAR(32) NOT NULL,
    event VARCHAR(32) NOT NULL,
    triggered_by BIGINT,
    comment TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_transitions_instance ON workflow_schema.workflow_transitions(workflow_instance_id);
