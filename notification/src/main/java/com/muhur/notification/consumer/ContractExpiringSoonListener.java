package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.ContractExpiringSoonPayload;
import com.muhur.notification.config.RabbitMQConfig;
import com.muhur.notification.domain.enums.EmailType;
import com.muhur.notification.service.EmailService;
import com.muhur.notification.service.IdempotencyService;
import com.muhur.notification.service.TemplateService;
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
    public void onMessage(DomainEvent<ContractExpiringSoonPayload> event) {
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
            case 1 -> "Contract expires tomorrow: " + p.title();
            case 7 -> "Contract expires in 1 week: " + p.title();
            case 30 -> "Contract expires in 30 days: " + p.title();
            default -> "Contract expires soon: " + p.title();
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
