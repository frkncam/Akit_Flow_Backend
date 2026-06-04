package com.muhur.template.service;

import com.muhur.common.client.dto.TemplateDraftResponse;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.template.dto.request.TemplateGenerateRequest;

public interface TemplateGenerationService {

    TemplateDraftResponse generate(TemplateGenerateRequest request, HeaderPrincipal user);
}
