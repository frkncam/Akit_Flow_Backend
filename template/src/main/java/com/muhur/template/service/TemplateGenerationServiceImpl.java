package com.muhur.template.service;

import com.muhur.common.client.AiClient;
import com.muhur.common.client.dto.TemplateDraftRequest;
import com.muhur.common.client.dto.TemplateDraftResponse;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.template.dto.request.TemplateGenerateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateGenerationServiceImpl implements TemplateGenerationService {

    private final AiClient aiClient;

    @Override
    public TemplateDraftResponse generate(TemplateGenerateRequest request, HeaderPrincipal user) {
        log.info("AI sablon uretimi istendi. userId={}, orgId={}, promptLength={}",
                user.userId(), user.organizationId(), request.prompt().length());

        TemplateDraftRequest aiRequest = new TemplateDraftRequest(
                request.prompt(),
                request.language() != null ? request.language() : "tr"
        );

        TemplateDraftResponse response = aiClient.generateTemplate(
                aiRequest,
                user.userId(),
                user.organizationId(),
                user.email(),
                user.role()
        );

        log.info("AI sablon uretimi basarili. name={}, category={}, variableCount={}",
                response.name(), response.category(),
                response.variables() != null ? response.variables().size() : 0);

        return response;
    }
}
