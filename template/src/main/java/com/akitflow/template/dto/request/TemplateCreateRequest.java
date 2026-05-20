package com.akitflow.template.dto.request;

import com.akitflow.template.domain.TemplateCategory;
import com.akitflow.common.client.dto.TemplateVariableDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateCreateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        TemplateCategory category,
        @NotBlank String bodyHtml,
        @Valid List<TemplateVariableDto> variables
) {}
