package com.akitflow.signature.event.payload;

import java.util.List;

public record SignatureBatchCompletedPayload(
        Long contractId,
        String contractTitle,
        String creatorEmail,
        List<String> signerEmails
) {}
