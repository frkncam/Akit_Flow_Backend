package com.akitflow.common.client.dto;

import java.time.Instant;

public record ContractDto(
        Long id,
        String title,
        String status,
        Long organizationId,
        Long createdBy,
        String creatorEmail,
        Instant createdAt
) {}
