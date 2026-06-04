package com.muhur.notification.service;

import com.muhur.notification.domain.enums.EmailType;

import java.util.UUID;

public interface EmailService {

    void send(EmailType type, String to, String subject, String htmlBody, UUID eventId);
}
