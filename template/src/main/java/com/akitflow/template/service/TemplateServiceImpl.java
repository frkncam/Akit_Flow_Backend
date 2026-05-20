package com.akitflow.template.service;

import com.akitflow.template.domain.Template;
import com.akitflow.template.domain.TemplateCategory;
import com.akitflow.template.domain.TemplateVariableData;
import com.akitflow.template.dto.request.TemplateCreateRequest;
import com.akitflow.template.dto.request.TemplatePreviewRequest;
import com.akitflow.template.dto.request.TemplateUpdateRequest;
import com.akitflow.template.dto.response.SystemVariableResponse;
import com.akitflow.template.dto.response.TemplatePreviewResponse;
import com.akitflow.common.client.dto.TemplateDto;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.template.mapper.TemplateMapper;
import com.akitflow.template.repository.TemplateRepository;
import com.akitflow.common.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository repository;
    private final TemplateMapper mapper;
    private final TemplateRenderer renderer;

    @Override
    @Transactional
    public TemplateDto create(TemplateCreateRequest request, HeaderPrincipal user) {
        Template entity = mapper.toEntity(request);
        entity.setOrganizationId(user.organizationId());
        entity.setCreatedBy(user.userId());
        if (request.variables() != null) {
            entity.setVariables(mapper.toDataList(request.variables()));
        } else {
            entity.setVariables(new ArrayList<>());
        }
        Template saved = repository.save(entity);
        log.info("Template created: id={}, orgId={}", saved.getId(), saved.getOrganizationId());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> list(HeaderPrincipal user, TemplateCategory category) {
        List<Template> entities = category == null
                ? repository.findAllByOrganizationId(user.organizationId())
                : repository.findAllByOrganizationIdAndCategory(user.organizationId(), category);
        return entities.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateDto get(Long id, HeaderPrincipal user) {
        return mapper.toDto(findOwnedOrThrow(id, user));
    }

    @Override
    @Transactional
    public TemplateDto update(Long id, TemplateUpdateRequest request, HeaderPrincipal user) {
        Template entity = findOwnedOrThrow(id, user);
        mapper.updateEntity(entity, request);
        if (request.variables() != null) {
            entity.setVariables(mapper.toDataList(request.variables()));
        }
        return mapper.toDto(repository.save(entity));
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
