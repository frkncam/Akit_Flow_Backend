package com.akitflow.auth.event;

import java.time.Instant;
import java.util.UUID;

public record AuthEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {
    public static <T> AuthEvent<T> of(String eventType, Long organizationId, Long actorId, T payload) {
        return new AuthEvent<>(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                organizationId,
                actorId,
                payload
        );
    }
}
