package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.SignatureBatchRejectedPayload;
import com.muhur.notification.config.RabbitMQConfig;
import com.muhur.notification.domain.enums.EmailType;
import com.muhur.notification.service.EmailService;
import com.muhur.notification.service.IdempotencyService;
import com.muhur.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureBatchRejectedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_BATCH_REJECTED)
    public void onMessage(DomainEvent<SignatureBatchRejectedPayload> event) {
        log.info("signature.batch.rejected received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        SignatureBatchRejectedPayload p = event.payload();
        if (p.creatorEmail() == null || p.creatorEmail().isBlank()) {
            log.warn("signature.batch.rejected has no creator email, skipping: contractId={}", p.contractId());
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("contractTitle", p.contractTitle());
        model.put("contractId", p.contractId());
        model.put("signerName", p.signerName());
        model.put("signerEmail", p.signerEmail());
        model.put("reason", p.reason() != null ? p.reason() : "(not specified)");

        String html = templates.render("contract-signature-rejected", model);

        emails.send(
                EmailType.CONTRACT_SIGNATURE_REJECTED,
                p.creatorEmail(),
                "Signature rejected: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
