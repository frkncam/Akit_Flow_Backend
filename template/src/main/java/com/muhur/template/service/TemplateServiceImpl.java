package com.muhur.template.service;

import com.muhur.common.client.dto.TemplateDto;
import com.muhur.common.exception.ResourceNotFoundException;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.template.domain.Template;
import com.muhur.template.domain.TemplateVariableData;
import com.muhur.template.dto.request.TemplateCreateRequest;
import com.muhur.template.dto.request.TemplatePreviewRequest;
import com.muhur.template.dto.request.TemplateUpdateRequest;
import com.muhur.template.dto.response.SystemVariableResponse;
import com.muhur.template.dto.response.TemplatePreviewResponse;
import com.muhur.template.mapper.TemplateMapper;
import com.muhur.template.repository.TemplateRepository;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<TemplateDto> search(Predicate predicate, Pageable pageable, HeaderPrincipal user) {
        return repository.findAll(predicate, pageable).map(mapper::toDto);
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
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: id=" + id));
    }
}
