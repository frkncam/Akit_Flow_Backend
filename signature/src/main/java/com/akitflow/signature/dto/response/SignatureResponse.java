package com.akitflow.signature.dto.response;

import com.akitflow.signature.domain.enums.SignatureStatus;

import java.time.Instant;

public record SignatureResponse(
        Long id,
        Long contractId,
        Long fileId,
        String signerName,
        String signerEmail,
        SignatureStatus status,
        String providerName,
        Instant requestedAt,
        Instant signedAt,
        Instant rejectedAt,
        String rejectionReason,
        Instant expiresAt,
        String signedFileStorageKey,
        String signatureMetadata
) {}
