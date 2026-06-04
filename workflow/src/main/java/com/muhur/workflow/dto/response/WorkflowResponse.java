package com.muhur.workflow.dto.response;

import java.time.Instant;
import java.util.List;

public record WorkflowResponse(
        Long id,
        Long contractId,
        String currentState,
        List<ApprovalStepResponse> steps,
        List<WorkflowTransitionResponse> transitions,
        Instant createdAt,
        Instant updatedAt
) {}
