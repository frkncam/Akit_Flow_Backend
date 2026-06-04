package com.muhur.workflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproverStepRequest(
        @NotNull Long approverUserId,
        @NotBlank String approverName,
        @NotBlank @Email String approverEmail
) {}
