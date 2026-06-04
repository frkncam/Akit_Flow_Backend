package com.muhur.ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Groq groq,
        RateLimit rateLimit,
        Validation validation,
        Prompt prompt
) {
    public record Groq(
            @NotBlank String primaryModel,
            @NotBlank String fallbackModel
    ) {}

    public record RateLimit(
            int requestsPerHour
    ) {}

    public record Validation(
            int maxVariables,
            int minVariables,
            int maxBodySizeBytes
    ) {}

    public record Prompt(
            @NotBlank String defaultKey
    ) {}
}
