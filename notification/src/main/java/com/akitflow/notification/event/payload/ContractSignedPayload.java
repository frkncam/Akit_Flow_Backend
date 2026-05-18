package com.akitflow.notification.event.payload;

import java.util.List;

public record ContractSignedPayload(
        Long contractId,
        String contractTitle,
        String creatorEmail,
        List<String> signerEmails
) {}
