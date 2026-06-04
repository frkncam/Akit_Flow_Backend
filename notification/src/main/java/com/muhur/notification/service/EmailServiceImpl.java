package com.muhur.notification.service;

import com.muhur.notification.config.AppProperties;
import com.muhur.notification.domain.EmailLog;
import com.muhur.notification.domain.enums.EmailStatus;
import com.muhur.notification.domain.enums.EmailType;
import com.muhur.notification.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final AppProperties appProperties;

    @Override
    public void send(EmailType type, String to, String subject, String htmlBody, UUID eventId) {
        EmailLog logEntry = EmailLog.builder()
                .eventId(eventId)
                .emailType(type)
                .recipient(to)
                .subject(subject)
                .attemptCount(1)
                .createdAt(Instant.now())
                .build();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(appProperties.mail().from(), appProperties.mail().fromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

            logEntry.setStatus(EmailStatus.SENT);
            logEntry.setSentAt(Instant.now());
            emailLogRepository.save(logEntry);

            log.info("Email sent: type={} to={} eventId={}", type, to, eventId);
        } catch (Exception e) {
            logEntry.setStatus(EmailStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            emailLogRepository.save(logEntry);

            log.error("Email delivery failed: type={} to={} eventId={}", type, to, eventId, e);
            // Re-throw to trigger Spring Retry / DLQ
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}
