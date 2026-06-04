package com.muhur.common.event.payload;

public record WorkflowTransitionedPayload(
        Long contractId,
        String fromState,
        String toState,
        String event
) {}
