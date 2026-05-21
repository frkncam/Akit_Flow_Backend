package com.akitflow.audit.dto.response;

import com.akitflow.audit.domain.enums.AggregateType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        Long id,
        UUID eventId,
        String eventType,
        AggregateType aggregateType,
        Long aggregateId,
        Long organizationId,
        Long actorId,
        JsonNode payload,
        Instant occurredAt,
        Instant recordedAt
) {}
