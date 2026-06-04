package com.muhur.common.client.dto;

public record GenerationMetadata(
        String model,
        int tokensUsed,
        long durationMs
) {}
