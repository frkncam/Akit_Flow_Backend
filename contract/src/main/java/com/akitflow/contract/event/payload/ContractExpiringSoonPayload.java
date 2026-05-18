package com.akitflow.contract.event.payload;

import java.time.LocalDate;

public record ContractExpiringSoonPayload(
        Long contractId,
        String title,
        LocalDate endDate,
        int daysRemaining,
        String creatorEmail
) {}
