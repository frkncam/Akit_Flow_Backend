package com.akitflow.contract.dto.response;

import com.akitflow.contract.domain.TemplateCategory;
import com.akitflow.contract.dto.common.TemplateVariableDto;

import java.time.Instant;
import java.util.List;

public record ContractTemplateResponse(
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
