package com.muhur.workflow.dto.response;

import java.time.Instant;

public record WorkflowTransitionResponse(
        Long id,
        String fromState,
        String toState,
        String event,
        Long triggeredBy,
        String comment,
        Instant occurredAt
) {}
