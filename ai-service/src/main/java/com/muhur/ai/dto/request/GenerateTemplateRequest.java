package com.muhur.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateTemplateRequest(
        @NotBlank @Size(max = 2000) String prompt,
        @Size(max = 10) String language
) {}
