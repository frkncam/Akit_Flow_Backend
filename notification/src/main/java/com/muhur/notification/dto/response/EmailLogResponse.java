package com.muhur.notification.dto.response;

import com.muhur.notification.domain.enums.EmailStatus;
import com.muhur.notification.domain.enums.EmailType;

import java.time.Instant;
import java.util.UUID;

public record EmailLogResponse(
        Long id,
        UUID eventId,
        EmailType emailType,
        String recipient,
        String subject,
        EmailStatus status,
        String errorMessage,
        Integer attemptCount,
        Instant sentAt,
        Instant createdAt
) {}
