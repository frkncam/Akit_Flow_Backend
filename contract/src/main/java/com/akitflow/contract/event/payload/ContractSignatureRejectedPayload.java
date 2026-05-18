package com.akitflow.contract.event.payload;

public record ContractSignatureRejectedPayload(
        Long contractId,
        String contractTitle,
        String creatorEmail,
        String signerName,
        String signerEmail,
        String reason
) {}
