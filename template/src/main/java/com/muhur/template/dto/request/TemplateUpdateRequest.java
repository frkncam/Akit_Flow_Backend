package com.muhur.template.dto.request;

import com.muhur.template.domain.TemplateCategory;
import com.muhur.common.client.dto.TemplateVariableDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateUpdateRequest(
        @Size(max = 255) String name,
        String description,
        TemplateCategory category,
        String bodyHtml,
        @Valid List<TemplateVariableDto> variables
) {}
