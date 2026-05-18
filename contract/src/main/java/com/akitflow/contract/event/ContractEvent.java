package com.akitflow.contract.event;

import java.time.Instant;
import java.util.UUID;

public record ContractEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {
    public static <T> ContractEvent<T> of(String eventType, Long organizationId, Long actorId, T payload) {
        return new ContractEvent<>(UUID.randomUUID(), eventType, Instant.now(), organizationId, actorId, payload);
    }
}
