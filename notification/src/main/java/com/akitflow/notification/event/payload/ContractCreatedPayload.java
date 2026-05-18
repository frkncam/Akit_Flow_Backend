package com.akitflow.notification.event.payload;

import java.util.List;

public record ContractCreatedPayload(
        Long contractId,
        String title,
        String contractType,
        List<String> partyEmails,
        String creatorEmail
) {}
