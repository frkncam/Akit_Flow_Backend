package com.akitflow.common.client.dto;

import java.time.Instant;

public record SignatureDto(
        Long id,
        Long contractId,
        Long fileId,
        String signerName,
        String signerEmail,
        String status,
        String providerName,
        Instant requestedAt,
        Instant signedAt,
        Instant rejectedAt,
        String rejectionReason,
        Instant expiresAt,
        String signedFileStorageKey,
        String signatureMetadata
) {}
