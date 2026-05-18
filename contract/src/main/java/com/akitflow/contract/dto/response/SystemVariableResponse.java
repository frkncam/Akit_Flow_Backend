package com.akitflow.contract.dto.response;

import com.akitflow.contract.domain.TemplateVariableType;

public record SystemVariableResponse(
        String key,
        String label,
        TemplateVariableType type
) {}
