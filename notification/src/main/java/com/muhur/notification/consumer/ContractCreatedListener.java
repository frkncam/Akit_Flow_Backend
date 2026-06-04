package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.ContractCreatedPayload;
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
public class ContractCreatedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_CREATED)
    public void onMessage(DomainEvent<ContractCreatedPayload> event) {
        log.info("contract.created alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        ContractCreatedPayload p = event.payload();
        String recipient = (p.creatorEmail() != null && !p.creatorEmail().isBlank())
                ? p.creatorEmail()
                : (p.partyEmails() != null && !p.partyEmails().isEmpty() ? p.partyEmails().get(0) : null);

        if (recipient == null) {
            log.warn("contract.created için alıcı belirlenemedi, atlanıyor: contractId={}", p.contractId());
            return;
        }

        String html = templates.render("contract-created", Map.of(
                "title", p.title(),
                "contractType", p.contractType() != null ? p.contractType() : "",
                "contractId", p.contractId()
        ));

        emails.send(
                EmailType.CONTRACT_CREATED,
                recipient,
                "New contract created: " + p.title(),
                html,
                event.eventId()
        );
    }
}
