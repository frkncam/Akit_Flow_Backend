package com.muhur.common.client;

import com.muhur.common.client.dto.TemplateDraftRequest;
import com.muhur.common.client.dto.TemplateDraftResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ai-service")
public interface AiClient {

    @PostMapping("/api/v1/ai/generate-template")
    TemplateDraftResponse generateTemplate(
            @RequestBody TemplateDraftRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Org-Id") Long orgId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Role") String role
    );
}
