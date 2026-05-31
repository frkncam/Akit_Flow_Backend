package com.akitflow.template.controller;

import com.akitflow.common.query.CommonPredicate;
import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.common.client.dto.TemplateDto;
import com.akitflow.common.web.PageResponse;
import com.akitflow.template.domain.Template;
import com.akitflow.template.dto.request.TemplateCreateRequest;
import com.akitflow.template.dto.request.TemplatePreviewRequest;
import com.akitflow.template.dto.request.TemplateUpdateRequest;
import com.akitflow.template.dto.response.SystemVariableResponse;
import com.akitflow.template.dto.response.TemplatePreviewResponse;
import com.akitflow.template.service.TemplateService;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService service;

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
