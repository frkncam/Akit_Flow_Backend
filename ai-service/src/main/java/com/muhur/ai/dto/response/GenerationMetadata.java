package com.muhur.ai.dto.response;

public record GenerationMetadata(
        String model,
        int tokensUsed,
        long durationMs
) {}
