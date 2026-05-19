package com.akitflow.signature.event;

import java.time.Instant;
import java.util.UUID;

public record SignatureEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {
    public static <T> SignatureEvent<T> of(String eventType, Long organizationId, Long actorId, T payload) {
        return new SignatureEvent<>(UUID.randomUUID(), eventType, Instant.now(), organizationId, actorId, payload);
    }
}
