package com.muhur.ai.controller;

import com.muhur.ai.dto.request.GenerateTemplateRequest;
import com.muhur.ai.dto.response.GenerateTemplateResponse;
import com.muhur.ai.service.TemplateGeneratorService;
import com.muhur.common.security.HeaderPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final TemplateGeneratorService templateGeneratorService;

    @PostMapping("/generate-template")
    public ResponseEntity<GenerateTemplateResponse> generateTemplate(
            @Valid @RequestBody GenerateTemplateRequest request,
            @AuthenticationPrincipal HeaderPrincipal user) {

        GenerateTemplateResponse response = templateGeneratorService.generate(
                request, user.userId(), user.organizationId());

        return ResponseEntity.ok(response);
    }
}
