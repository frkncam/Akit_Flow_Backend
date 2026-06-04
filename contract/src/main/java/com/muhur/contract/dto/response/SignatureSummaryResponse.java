package com.muhur.contract.dto.response;

import com.muhur.common.client.dto.SignatureDto;

import java.time.Instant;

public record SignatureSummaryResponse(
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
        Instant expiresAt
) {

    public static SignatureSummaryResponse from(SignatureDto dto) {
        return new SignatureSummaryResponse(
                dto.id(),
                dto.contractId(),
                dto.fileId(),
                dto.signerName(),
                dto.signerEmail(),
                dto.status(),
                dto.providerName(),
                dto.requestedAt(),
                dto.signedAt(),
                dto.rejectedAt(),
                dto.rejectionReason(),
                dto.expiresAt()
        );
    }
}
