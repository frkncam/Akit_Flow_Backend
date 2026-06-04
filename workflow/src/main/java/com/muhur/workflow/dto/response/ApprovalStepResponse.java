package com.muhur.workflow.dto.response;

import java.time.Instant;

public record ApprovalStepResponse(
        Long id,
        Integer orderIndex,
        Long approverUserId,
        String approverName,
        String approverEmail,
        String status,
        String comment,
        Instant decidedAt
) {}
