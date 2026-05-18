package com.akitflow.contract.dto.response;

import com.akitflow.contract.domain.enums.SignatureStatus;

import java.time.Instant;

public record SignatureViewResponse(
        String contractTitle,
        String signerName,
        String signerEmail,
        String downloadUrl,
        String signedDownloadUrl,
        Instant expiresAt,
        SignatureStatus status,
        String signatureMetadata
) {}
