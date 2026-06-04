package com.muhur.contract.dto.response;

import java.time.Instant;

public record ContractFileResponse(
        Long id,
        Long contractId,
        String fileName,
        String contentType,
        Long size,
        Integer version,
        Long uploadedBy,
        Instant uploadedAt,
        String downloadUrl
) {}
