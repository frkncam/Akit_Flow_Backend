package com.akitflow.contract.controller;

import com.akitflow.contract.domain.TemplateCategory;
import com.akitflow.contract.dto.request.ContractTemplateCreateRequest;
import com.akitflow.contract.dto.request.ContractTemplateUpdateRequest;
import com.akitflow.contract.dto.request.TemplatePreviewRequest;
import com.akitflow.contract.dto.response.ContractTemplateResponse;
import com.akitflow.contract.dto.response.SystemVariableResponse;
import com.akitflow.contract.dto.response.TemplatePreviewResponse;
import com.akitflow.contract.security.HeaderPrincipal;
import com.akitflow.contract.service.ContractTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contract-templates")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final ContractTemplateService service;

    @PostMapping
    public ResponseEntity<ContractTemplateResponse> create(@Valid @RequestBody ContractTemplateCreateRequest request,
                                                           @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, user));
    }

    @GetMapping
    public List<ContractTemplateResponse> list(@RequestParam(value = "category", required = false) TemplateCategory category,
                                               @AuthenticationPrincipal HeaderPrincipal user) {
        return service.list(user, category);
    }

    @GetMapping("/system-variables")
    public List<SystemVariableResponse> systemVariables() {
        return service.systemVariables();
    }

    @GetMapping("/{id}")
    public ContractTemplateResponse get(@PathVariable Long id,
                                        @AuthenticationPrincipal HeaderPrincipal user) {
        return service.get(id, user);
    }

    @PatchMapping("/{id}")
    public ContractTemplateResponse update(@PathVariable Long id,
                                           @Valid @RequestBody ContractTemplateUpdateRequest request,
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
