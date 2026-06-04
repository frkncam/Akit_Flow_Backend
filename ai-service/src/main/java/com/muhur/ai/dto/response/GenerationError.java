package com.muhur.ai.dto.response;

import java.util.List;

public record GenerationError(
        String message,
        List<String> details
) {}
