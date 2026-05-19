package com.akitflow.notification.event;

import java.time.Instant;
import java.util.UUID;

public record SignatureEventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long organizationId,
        Long actorId,
        T payload
) {}
