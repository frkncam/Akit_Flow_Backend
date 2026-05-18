package com.akitflow.notification.event.payload;

import java.time.LocalDate;

public record ContractExpiringSoonPayload(
        Long contractId,
        String title,
        LocalDate endDate,
        int daysRemaining,
        String creatorEmail
) {}
