package com.akitflow.common.event.payload;

public record SignatureBatchRejectedPayload(
        Long contractId,
        String contractTitle,
        String creatorEmail,
        String signerName,
        String signerEmail,
        String reason
) {}
