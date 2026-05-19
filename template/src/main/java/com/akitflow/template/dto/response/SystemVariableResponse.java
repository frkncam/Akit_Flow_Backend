package com.akitflow.template.dto.response;

import com.akitflow.template.domain.TemplateVariableType;

public record SystemVariableResponse(
        String key,
        String label,
        TemplateVariableType type
) {}
