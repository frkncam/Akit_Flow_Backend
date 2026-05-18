package com.akitflow.contract.dto.common;

import com.akitflow.contract.domain.TemplateVariableSource;
import com.akitflow.contract.domain.TemplateVariableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateVariableDto(
        @NotBlank @Size(max = 64) String key,
        @NotBlank @Size(max = 128) String label,
        @NotNull TemplateVariableType type,
        @NotNull TemplateVariableSource source,
        String systemBinding,
        String defaultValue,
        boolean required,
        int position
) {}
