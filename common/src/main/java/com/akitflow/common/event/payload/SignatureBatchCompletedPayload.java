package com.akitflow.common.event.payload;

import java.util.List;

public record SignatureBatchCompletedPayload(
        Long contractId,
        String contractTitle,
        String creatorEmail,
        List<String> signerEmails
) {}
