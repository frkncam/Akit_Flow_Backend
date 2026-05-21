package com.akitflow.workflow.dto.response;

import java.time.Instant;

public record PendingApprovalResponse(
        Long stepId,
        Long workflowInstanceId,
        Long contractId,
        String contractTitle,
        Integer orderIndex,
        Integer totalSteps,
        Instant requestedAt
) {}
