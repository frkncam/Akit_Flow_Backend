--liquibase formatted sql

--changeset furkan:009-processed-events
CREATE TABLE contract_schema.processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
