package com.muhur.common.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateDraftRequest(
        @NotBlank @Size(max = 2000) String prompt,
        @Size(max = 10) String language
) {}
