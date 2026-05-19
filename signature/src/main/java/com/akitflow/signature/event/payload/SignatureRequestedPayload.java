package com.akitflow.signature.event.payload;

import java.time.Instant;

public record SignatureRequestedPayload(
        Long contractId,
        String contractTitle,
        String signerName,
        String signerEmail,
        String token,
        Instant expiresAt
) {}
