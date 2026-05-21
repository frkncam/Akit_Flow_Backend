--liquibase formatted sql

--changeset furkan:003-update-aggregate-type-constraint
ALTER TABLE audit_schema.audit_events
    DROP CONSTRAINT chk_audit_aggregate_type,
    ADD CONSTRAINT chk_audit_aggregate_type
        CHECK (aggregate_type IN ('CONTRACT','USER','WORKFLOW','SIGNATURE'));

--changeset furkan:004-improve-actor-index
DROP INDEX audit_schema.idx_audit_org_actor;
CREATE INDEX idx_audit_org_actor_recorded
    ON audit_schema.audit_events(organization_id, actor_id, recorded_at DESC);
