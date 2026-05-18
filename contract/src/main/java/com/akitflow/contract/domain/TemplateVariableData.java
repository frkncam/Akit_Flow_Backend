package com.akitflow.contract.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateVariableData(
        String key,
        String label,
        TemplateVariableType type,
        TemplateVariableSource source,
        String systemBinding,
        String defaultValue,
        boolean required,
        int position
) {}
