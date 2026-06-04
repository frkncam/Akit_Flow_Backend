package com.muhur.template.service;

import com.muhur.common.security.HeaderPrincipal;
import com.muhur.common.client.dto.TemplateDto;
import com.muhur.template.dto.request.TemplateCreateRequest;
import com.muhur.template.dto.request.TemplatePreviewRequest;
import com.muhur.template.dto.request.TemplateUpdateRequest;
import com.muhur.template.dto.response.SystemVariableResponse;
import com.muhur.template.dto.response.TemplatePreviewResponse;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TemplateService {

    TemplateDto create(TemplateCreateRequest request, HeaderPrincipal user);

    Page<TemplateDto> search(Predicate predicate, Pageable pageable, HeaderPrincipal user);

    TemplateDto get(Long id, HeaderPrincipal user);

    TemplateDto update(Long id, TemplateUpdateRequest request, HeaderPrincipal user);

    void delete(Long id, HeaderPrincipal user);

    List<SystemVariableResponse> systemVariables();

    TemplatePreviewResponse preview(Long id, TemplatePreviewRequest request, HeaderPrincipal user);
}
