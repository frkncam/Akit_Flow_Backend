package com.akitflow.notification.event.payload;

public record ContractStatusChangedPayload(
        Long contractId,
        String title,
        String oldStatus,
        String newStatus,
        String actorEmail
) {}
