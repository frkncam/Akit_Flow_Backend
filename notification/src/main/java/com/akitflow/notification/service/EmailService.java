package com.akitflow.notification.service;

import com.akitflow.notification.domain.enums.EmailType;

import java.util.UUID;

public interface EmailService {

    void send(EmailType type, String to, String subject, String htmlBody, UUID eventId);
}
