--liquibase formatted sql

--changeset furkan:002-audit-append-only-trigger endDelimiter:;;
CREATE OR REPLACE FUNCTION audit_schema.reject_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_events append-only tablosudur: % islemi reddedildi', TG_OP;
END;
$$ LANGUAGE plpgsql;;

CREATE TRIGGER trg_audit_events_no_modify
BEFORE UPDATE OR DELETE ON audit_schema.audit_events
FOR EACH ROW EXECUTE FUNCTION audit_schema.reject_audit_modification();;
