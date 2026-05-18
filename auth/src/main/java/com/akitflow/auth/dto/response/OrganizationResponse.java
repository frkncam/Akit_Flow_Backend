package com.akitflow.auth.dto.response;

import java.time.Instant;

public record OrganizationResponse(
        Long id,
        String name,
        String slug,
        Instant createdAt,
        Instant updatedAt
) {}
