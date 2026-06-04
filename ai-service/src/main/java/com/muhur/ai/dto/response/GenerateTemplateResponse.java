package com.muhur.ai.dto.response;

import com.muhur.common.client.dto.TemplateVariableDto;

import java.util.List;

public record GenerateTemplateResponse(
        String name,
        String description,
        String category,
        String bodyHtml,
        List<TemplateVariableDto> variables,
        GenerationMetadata metadata
) {}
