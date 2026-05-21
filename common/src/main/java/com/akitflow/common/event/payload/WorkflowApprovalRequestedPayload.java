package com.akitflow.common.event.payload;

public record WorkflowApprovalRequestedPayload(
        Long contractId,
        String contractTitle,
        String approverName,
        String approverEmail,
        int stepOrder,
        int totalSteps
) {}
