package com.muhur.template.controller;

import com.muhur.common.query.CommonPredicate;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.common.client.dto.TemplateDto;
import com.muhur.common.client.dto.TemplateDraftResponse;
import com.muhur.common.web.PageResponse;
import com.muhur.template.domain.Template;
import com.muhur.template.dto.request.TemplateCreateRequest;
import com.muhur.template.dto.request.TemplateGenerateRequest;
import com.muhur.template.dto.request.TemplatePreviewRequest;
import com.muhur.template.dto.request.TemplateUpdateRequest;
import com.muhur.template.dto.response.SystemVariableResponse;
import com.muhur.template.dto.response.TemplatePreviewResponse;
import com.muhur.template.service.TemplateGenerationService;
import com.muhur.template.service.TemplateService;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService service;
    private final TemplateGenerationService templateGenerationService;

    public TemplateController(TemplateService service, TemplateGenerationService templateGenerationService) {
        this.service = service;
        this.templateGenerationService = templateGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<TemplateDraftResponse> generate(
            @Valid @RequestBody TemplateGenerateRequest request,
            @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.ok(templateGenerationService.generate(request, user));
    }

    @PostMapping
    public ResponseEntity<TemplateDto> create(@Valid @RequestBody TemplateCreateRequest request,
                                                   @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, user));
    }

    @GetMapping
    public PageResponse<TemplateDto> list(
            @CommonPredicate(root = Template.class) Predicate predicate,
            Pageable pageable,
            @AuthenticationPrincipal HeaderPrincipal user) {
        return PageResponse.from(service.search(predicate, pageable, user));
    }

    @GetMapping("/system-variables")
    public List<SystemVariableResponse> systemVariables() {
        return service.systemVariables();
    }

    @GetMapping("/{id}")
    public TemplateDto get(@PathVariable Long id,
                                @AuthenticationPrincipal HeaderPrincipal user) {
        return service.get(id, user);
    }

    @PatchMapping("/{id}")
    public TemplateDto update(@PathVariable Long id,
                                   @Valid @RequestBody TemplateUpdateRequest request,
                                   @AuthenticationPrincipal HeaderPrincipal user) {
        return service.update(id, request, user);
    }

    @PostMapping("/{id}/preview")
    public TemplatePreviewResponse preview(@PathVariable Long id,
                                           @RequestBody(required = false) TemplatePreviewRequest request,
                                           @AuthenticationPrincipal HeaderPrincipal user) {
        return service.preview(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal HeaderPrincipal user) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
