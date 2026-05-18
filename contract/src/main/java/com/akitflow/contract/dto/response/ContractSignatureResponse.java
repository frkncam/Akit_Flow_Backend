package com.akitflow.contract.dto.response;

import com.akitflow.contract.domain.enums.SignatureStatus;

import java.time.Instant;

public record ContractSignatureResponse(
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
