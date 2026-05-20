package com.akitflow.common.client.dto;

import java.time.Instant;
import java.util.List;

public record TemplateDto(
        Long id,
        Long organizationId,
        String name,
        String description,
        String category,
        String bodyHtml,
        List<TemplateVariableDto> variables,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
