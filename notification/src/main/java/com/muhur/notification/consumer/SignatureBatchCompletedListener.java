package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.SignatureBatchCompletedPayload;
import com.muhur.notification.config.RabbitMQConfig;
import com.muhur.notification.domain.enums.EmailType;
import com.muhur.notification.service.EmailService;
import com.muhur.notification.service.IdempotencyService;
import com.muhur.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureBatchCompletedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_BATCH_COMPLETED)
    public void onMessage(DomainEvent<SignatureBatchCompletedPayload> event) {
        log.info("signature.batch.completed received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        SignatureBatchCompletedPayload p = event.payload();

        List<String> recipients = new ArrayList<>();
        if (p.creatorEmail() != null && !p.creatorEmail().isBlank()) {
            recipients.add(p.creatorEmail());
        }
        if (p.signerEmails() != null) {
            p.signerEmails().stream()
                    .filter(e -> e != null && !e.isBlank())
                    .filter(e -> !recipients.contains(e))
                    .forEach(recipients::add);
        }

        if (recipients.isEmpty()) {
            log.warn("signature.batch.completed has no recipients, skipping: contractId={}", p.contractId());
            return;
        }

        String html = templates.render("contract-signed", Map.of(
                "contractTitle", p.contractTitle(),
                "contractId", p.contractId(),
                "signerEmails", p.signerEmails() == null ? List.of() : p.signerEmails()
        ));

        for (String to : recipients) {
            emails.send(
                    EmailType.CONTRACT_SIGNED,
                    to,
                    "Contract signed: " + p.contractTitle(),
                    html,
                    event.eventId()
            );
        }
    }
}
