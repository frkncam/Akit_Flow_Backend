package com.akitflow.common.event.payload;

public record WorkflowApprovedPayload(
        Long contractId,
        String contractTitle,
        String ownerEmail
) {}
