package com.akitflow.template.service;

import com.akitflow.template.domain.TemplateCategory;
import com.akitflow.template.dto.request.TemplateCreateRequest;
import com.akitflow.template.dto.request.TemplatePreviewRequest;
import com.akitflow.template.dto.request.TemplateUpdateRequest;
import com.akitflow.template.dto.response.SystemVariableResponse;
import com.akitflow.template.dto.response.TemplatePreviewResponse;
import com.akitflow.common.client.dto.TemplateDto;
import com.akitflow.common.security.HeaderPrincipal;

import java.util.List;

public interface TemplateService {

    TemplateDto create(TemplateCreateRequest request, HeaderPrincipal user);

    List<TemplateDto> list(HeaderPrincipal user, TemplateCategory category);

    TemplateDto get(Long id, HeaderPrincipal user);

    TemplateDto update(Long id, TemplateUpdateRequest request, HeaderPrincipal user);

    void delete(Long id, HeaderPrincipal user);

    List<SystemVariableResponse> systemVariables();

    TemplatePreviewResponse preview(Long id, TemplatePreviewRequest request, HeaderPrincipal user);
}
