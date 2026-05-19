package com.akitflow.template.dto.common;

import com.akitflow.template.domain.TemplateVariableSource;
import com.akitflow.template.domain.TemplateVariableType;
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
