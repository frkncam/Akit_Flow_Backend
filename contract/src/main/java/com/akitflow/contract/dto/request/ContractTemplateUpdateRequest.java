package com.akitflow.contract.dto.request;

import com.akitflow.contract.domain.TemplateCategory;
import com.akitflow.contract.dto.common.TemplateVariableDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContractTemplateUpdateRequest(
        @Size(max = 255) String name,
        String description,
        TemplateCategory category,
        String bodyHtml,
        @Valid List<TemplateVariableDto> variables
) {}
