package com.akitflow.notification.event.payload;

import java.time.Instant;

public record ContractSignatureRequestedPayload(
        Long contractId,
        String contractTitle,
        String signerName,
        String signerEmail,
        String token,
        Instant expiresAt
) {}
