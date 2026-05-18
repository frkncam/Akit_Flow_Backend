package com.akitflow.notification.consumer;

import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.event.ContractEventEnvelope;
import com.akitflow.notification.event.payload.ContractExpiringSoonPayload;
import com.akitflow.notification.service.EmailService;
import com.akitflow.notification.service.IdempotencyService;
import com.akitflow.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractExpiringSoonListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_EXPIRING_SOON)
    public void onMessage(ContractEventEnvelope<ContractExpiringSoonPayload> event) {
        log.info("contract.expiring.soon alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        ContractExpiringSoonPayload p = event.payload();
        if (p.creatorEmail() == null || p.creatorEmail().isBlank()) {
            log.warn("contract.expiring.soon için alıcı email yok, atlanıyor: contractId={}", p.contractId());
            return;
        }

        String html = templates.render("contract-expiring-soon", Map.of(
                "title", p.title(),
                "endDate", p.endDate(),
                "daysRemaining", p.daysRemaining(),
                "contractId", p.contractId()
        ));

        String subject = switch (p.daysRemaining()) {
            case 1 -> "Sözleşme yarın sona eriyor: " + p.title();
            case 7 -> "Sözleşme 1 hafta içinde sona eriyor: " + p.title();
            case 30 -> "Sözleşme 30 gün içinde sona eriyor: " + p.title();
            default -> "Sözleşme yakında sona eriyor: " + p.title();
        };

        emails.send(
                EmailType.CONTRACT_EXPIRING_SOON,
                p.creatorEmail(),
                subject,
                html,
                event.eventId()
        );
    }
}
