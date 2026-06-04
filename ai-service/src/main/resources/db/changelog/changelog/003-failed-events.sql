--liquibase formatted sql

-- ============================================================
-- ai_schema: common modulundeki FailedEvent entity'si icin tablo
-- (RabbitMQ event publish hatalarinin kalici kaydi / retry)
-- ============================================================

--changeset akitflow:003-failed-events
CREATE TABLE ai_schema.failed_events (
    id                BIGSERIAL PRIMARY KEY,
    event_json        TEXT          NOT NULL,
    exchange          VARCHAR(128)  NOT NULL,
    routing_key       VARCHAR(128)  NOT NULL,
    event_type        VARCHAR(128)  NOT NULL,
    last_error        VARCHAR(1024),
    terminal_failure  BOOLEAN       NOT NULL DEFAULT FALSE,
    retry_count       INT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_retry_at     TIMESTAMPTZ
);

CREATE INDEX idx_failed_events_terminal ON ai_schema.failed_events(terminal_failure, created_at);
