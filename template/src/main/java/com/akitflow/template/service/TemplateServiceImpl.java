package com.akitflow.template.service;

import com.akitflow.template.domain.Template;
import com.akitflow.template.domain.TemplateCategory;
import com.akitflow.template.domain.TemplateVariableData;
import com.akitflow.template.dto.request.TemplateCreateRequest;
import com.akitflow.template.dto.request.TemplatePreviewRequest;
import com.akitflow.template.dto.request.TemplateUpdateRequest;
import com.akitflow.template.dto.response.SystemVariableResponse;
import com.akitflow.template.dto.response.TemplatePreviewResponse;
import com.akitflow.template.dto.response.TemplateResponse;
import com.akitflow.template.exception.ResourceNotFoundException;
import com.akitflow.template.mapper.TemplateMapper;
import com.akitflow.template.repository.TemplateRepository;
import com.akitflow.template.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository repository;
    private final TemplateMapper mapper;
    private final TemplateRenderer renderer;

    @Override
    @Transactional
    public TemplateResponse create(TemplateCreateRequest request, HeaderPrincipal user) {
        Template entity = mapper.toEntity(request);
        entity.setOrganizationId(user.organizationId());
        entity.setCreatedBy(user.userId());
        if (request.variables() != null) {
            entity.setVariables(mapper.toDataList(request.variables()));
        } else {
            entity.setVariables(new ArrayList<>());
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> list(HeaderPrincipal user, TemplateCategory category) {
        List<Template> entities = category == null
                ? repository.findAllByOrganizationId(user.organizationId())
                : repository.findAllByOrganizationIdAndCategory(user.organizationId(), category);
        return entities.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponse get(Long id, HeaderPrincipal user) {
        return mapper.toResponse(findOwnedOrThrow(id, user));
    }

    @Override
    @Transactional
    public TemplateResponse update(Long id, TemplateUpdateRequest request, HeaderPrincipal user) {
        Template entity = findOwnedOrThrow(id, user);
        mapper.updateEntity(entity, request);
        if (request.variables() != null) {
            entity.setVariables(mapper.toDataList(request.variables()));
        }
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id, HeaderPrincipal user) {
        Template entity = findOwnedOrThrow(id, user);
        repository.delete(entity);
    }

    @Override
    public List<SystemVariableResponse> systemVariables() {
        return TemplateSystemVariables.CATALOGUE;
    }

    @Override
    @Transactional(readOnly = true)
    public TemplatePreviewResponse preview(Long id, TemplatePreviewRequest request, HeaderPrincipal user) {
        Template template = findOwnedOrThrow(id, user);
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

    private Template findOwnedOrThrow(Long id, HeaderPrincipal user) {
        return repository.findByIdAndOrganizationId(id, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Template bulunamadı: id=" + id));
    }
}
