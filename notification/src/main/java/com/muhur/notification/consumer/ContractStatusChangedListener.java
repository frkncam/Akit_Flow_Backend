package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.ContractStatusChangedPayload;
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
public class ContractStatusChangedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_STATUS_CHANGED)
    public void onMessage(DomainEvent<ContractStatusChangedPayload> event) {
        log.info("contract.status.changed alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        ContractStatusChangedPayload p = event.payload();
        if (p.actorEmail() == null || p.actorEmail().isBlank()) {
            log.warn("contract.status.changed için alıcı email yok, atlanıyor: contractId={}", p.contractId());
            return;
        }

        String html = templates.render("contract-status-changed", Map.of(
                "title", p.title(),
                "oldStatus", p.oldStatus(),
                "newStatus", p.newStatus(),
                "contractId", p.contractId()
        ));

        emails.send(
                EmailType.CONTRACT_STATUS_CHANGED,
                p.actorEmail(),
                "Contract status updated: " + p.title(),
                html,
                event.eventId()
        );
    }
}
