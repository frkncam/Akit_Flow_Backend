package com.akitflow.common.client.dto;

public record TemplateVariableDto(
        String key,
        String label,
        String type,
        String source,
        String systemBinding,
        String defaultValue,
        boolean required,
        int position
) {}
