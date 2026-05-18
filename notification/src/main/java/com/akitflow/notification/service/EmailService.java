package com.akitflow.notification.service;

import com.akitflow.notification.config.AppProperties;
import com.akitflow.notification.domain.EmailLog;
import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.repository.EmailLogRepository;
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
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final AppProperties appProperties;

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

            log.info("Mail gönderildi: type={} to={} eventId={}", type, to, eventId);
        } catch (Exception e) {
            logEntry.setStatus(EmailStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            emailLogRepository.save(logEntry);

            log.error("Mail gönderimi başarısız: type={} to={} eventId={}", type, to, eventId, e);
            // Spring Retry / DLQ tetiklensin diye yeniden fırlat
            throw new RuntimeException("Mail gönderimi başarısız: " + e.getMessage(), e);
        }
    }
}
