package com.akitflow.common.event.payload;

import java.time.Instant;

public record SignatureOtpRequestedPayload(
        Long contractId,
        String contractTitle,
        String signerName,
        String signerEmail,
        String token,
        String code,
        Instant expiresAt
) {}
