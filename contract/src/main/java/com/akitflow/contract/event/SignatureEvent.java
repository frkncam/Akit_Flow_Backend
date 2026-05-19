package com.akitflow.contract.event;

import java.time.Instant;
import java.util.UUID;

public record SignatureEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {}
