package com.akitflow.notification.dto.response;

import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.domain.enums.EmailType;

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
