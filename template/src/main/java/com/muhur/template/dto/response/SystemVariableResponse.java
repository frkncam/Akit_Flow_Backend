package com.muhur.template.dto.response;

import com.muhur.template.domain.TemplateVariableType;

public record SystemVariableResponse(
        String key,
        String label,
        TemplateVariableType type
) {}
