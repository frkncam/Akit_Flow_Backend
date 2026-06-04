package com.muhur.ai.service;

import com.muhur.ai.dto.request.GenerateTemplateRequest;
import com.muhur.ai.dto.response.GenerateTemplateResponse;

public interface TemplateGeneratorService {

    GenerateTemplateResponse generate(GenerateTemplateRequest request, Long userId, Long orgId);
}
