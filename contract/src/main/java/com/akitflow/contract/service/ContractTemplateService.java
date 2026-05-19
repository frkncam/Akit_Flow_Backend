package com.akitflow.contract.service;

import com.akitflow.contract.domain.TemplateCategory;
import com.akitflow.contract.dto.request.ContractTemplateCreateRequest;
import com.akitflow.contract.dto.request.ContractTemplateUpdateRequest;
import com.akitflow.contract.dto.request.TemplatePreviewRequest;
import com.akitflow.contract.dto.response.ContractTemplateResponse;
import com.akitflow.contract.dto.response.SystemVariableResponse;
import com.akitflow.contract.dto.response.TemplatePreviewResponse;
import com.akitflow.contract.security.HeaderPrincipal;

import java.util.List;

public interface ContractTemplateService {

    ContractTemplateResponse create(ContractTemplateCreateRequest request, HeaderPrincipal user);

    List<ContractTemplateResponse> list(HeaderPrincipal user, TemplateCategory category);

    ContractTemplateResponse get(Long id, HeaderPrincipal user);

    ContractTemplateResponse update(Long id, ContractTemplateUpdateRequest request, HeaderPrincipal user);

    void delete(Long id, HeaderPrincipal user);

    List<SystemVariableResponse> systemVariables();

    TemplatePreviewResponse preview(Long id, TemplatePreviewRequest request, HeaderPrincipal user);
}
