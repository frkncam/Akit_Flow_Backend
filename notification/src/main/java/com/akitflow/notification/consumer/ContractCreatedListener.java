package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.ContractCreatedPayload;
import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
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
                "Yeni sözleşme oluşturuldu: " + p.title(),
                html,
                event.eventId()
        );
    }
}
