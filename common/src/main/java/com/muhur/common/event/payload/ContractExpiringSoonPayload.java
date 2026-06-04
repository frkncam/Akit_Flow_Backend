package com.muhur.common.event.payload;

import java.time.LocalDate;

public record ContractExpiringSoonPayload(
        Long contractId,
        String title,
        LocalDate endDate,
        int daysRemaining,
        String creatorEmail
) {}
