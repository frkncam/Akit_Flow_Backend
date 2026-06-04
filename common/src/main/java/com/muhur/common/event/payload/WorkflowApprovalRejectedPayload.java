package com.muhur.common.event.payload;

public record WorkflowApprovalRejectedPayload(
        Long contractId,
        String contractTitle,
        String ownerEmail,
        String rejectedByName,
        String reason
) {}
