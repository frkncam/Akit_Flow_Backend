package com.akitflow.contract.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "template-service")
public interface TemplateClient {

    @GetMapping("/api/v1/templates/{id}")
    TemplateDto getTemplate(@PathVariable Long id, @RequestHeader("X-Org-Id") Long orgId);

    record TemplateDto(
            Long id,
            Long organizationId,
            String name,
            String description,
            String category,
            String bodyHtml,
            List<VariableDto> variables,
            Long createdBy,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}

    record VariableDto(
            String key,
            String label,
            String type,
            String source,
            String systemBinding,
            String defaultValue,
            boolean required,
            int position
    ) {}
}
