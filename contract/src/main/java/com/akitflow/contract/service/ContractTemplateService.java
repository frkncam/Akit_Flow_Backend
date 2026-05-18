package com.akitflow.contract.service;

import com.akitflow.contract.domain.ContractTemplate;
import com.akitflow.contract.domain.TemplateCategory;
import com.akitflow.contract.domain.TemplateVariableData;
import com.akitflow.contract.dto.request.ContractTemplateCreateRequest;
import com.akitflow.contract.dto.request.ContractTemplateUpdateRequest;
import com.akitflow.contract.dto.request.TemplatePreviewRequest;
import com.akitflow.contract.dto.response.ContractTemplateResponse;
import com.akitflow.contract.dto.response.SystemVariableResponse;
import com.akitflow.contract.dto.response.TemplatePreviewResponse;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.mapper.ContractTemplateMapper;
import com.akitflow.contract.repository.ContractTemplateRepository;
import com.akitflow.contract.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContractTemplateService {

    private final ContractTemplateRepository repository;
    private final ContractTemplateMapper mapper;
    private final TemplateRenderer renderer;

    @Transactional
    public ContractTemplateResponse create(ContractTemplateCreateRequest request, HeaderPrincipal user) {
        ContractTemplate entity = mapper.toEntity(request);
        entity.setOrganizationId(user.organizationId());
        entity.setCreatedBy(user.userId());
        if (request.variables() != null) {
            entity.setVariables(mapper.toDataList(request.variables()));
        } else {
            entity.setVariables(new ArrayList<>());
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ContractTemplateResponse> list(HeaderPrincipal user, TemplateCategory category) {
        List<ContractTemplate> entities = category == null
                ? repository.findAllByOrganizationId(user.organizationId())
                : repository.findAllByOrganizationIdAndCategory(user.organizationId(), category);
        return entities.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContractTemplateResponse get(Long id, HeaderPrincipal user) {
        return mapper.toResponse(findOwnedOrThrow(id, user));
    }

    @Transactional
    public ContractTemplateResponse update(Long id, ContractTemplateUpdateRequest request, HeaderPrincipal user) {
        ContractTemplate entity = findOwnedOrThrow(id, user);
        mapper.updateEntity(entity, request);
        if (request.variables() != null) {
            entity.setVariables(mapper.toDataList(request.variables()));
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id, HeaderPrincipal user) {
        ContractTemplate entity = findOwnedOrThrow(id, user);
        repository.delete(entity);
    }

    public List<SystemVariableResponse> systemVariables() {
        return TemplateSystemVariables.CATALOGUE;
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse preview(Long id, TemplatePreviewRequest request, HeaderPrincipal user) {
        ContractTemplate template = findOwnedOrThrow(id, user);
        List<TemplateVariableData> variables = template.getVariables() != null
                ? template.getVariables()
                : Collections.emptyList();
        Map<String, String> custom = request != null && request.customValues() != null
                ? request.customValues()
                : Collections.emptyMap();
        Map<String, String> values = renderer.resolveValues(variables, Collections.emptyMap(), custom);
        String html = renderer.substitute(template.getBodyHtml(), values);
        return new TemplatePreviewResponse(html);
    }

    private ContractTemplate findOwnedOrThrow(Long id, HeaderPrincipal user) {
        return repository.findByIdAndOrganizationId(id, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Template bulunamadı: id=" + id));
    }
}
