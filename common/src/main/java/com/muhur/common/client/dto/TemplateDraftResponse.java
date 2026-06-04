package com.muhur.common.client.dto;

import java.util.List;

public record TemplateDraftResponse(
        String name,
        String description,
        String category,
        String bodyHtml,
        List<TemplateVariableDto> variables,
        GenerationMetadata metadata
) {}
