package com.akitflow.common.event;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {
    public static <T> DomainEvent<T> of(String eventType, Long organizationId, Long actorId, T payload) {
        return new DomainEvent<>(UUID.randomUUID(), eventType, Instant.now(), organizationId, actorId, payload);
    }
}
