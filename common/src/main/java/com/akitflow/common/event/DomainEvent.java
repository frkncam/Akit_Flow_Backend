package com.akitflow.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {
    public DomainEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        // actorId may be null for system-generated events (e.g. public signing callbacks)
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static <T> DomainEvent<T> of(String eventType, Long organizationId, Long actorId, T payload) {
        return new DomainEvent<>(UUID.randomUUID(), eventType, Instant.now(), organizationId, actorId, payload);
    }
}
