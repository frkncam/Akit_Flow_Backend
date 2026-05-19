package com.akitflow.template.dto.response;

import com.akitflow.template.domain.TemplateCategory;
import com.akitflow.template.dto.common.TemplateVariableDto;

import java.time.Instant;
import java.util.List;

public record TemplateResponse(
        Long id,
        Long organizationId,
        String name,
        String description,
        TemplateCategory category,
        String bodyHtml,
        List<TemplateVariableDto> variables,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
