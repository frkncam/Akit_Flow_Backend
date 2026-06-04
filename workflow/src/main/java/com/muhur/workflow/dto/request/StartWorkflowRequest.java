package com.muhur.workflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StartWorkflowRequest(
        @NotEmpty @Valid List<ApproverStepRequest> approvers
) {}
