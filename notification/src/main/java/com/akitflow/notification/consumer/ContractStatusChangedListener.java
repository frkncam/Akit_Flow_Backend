package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.ContractStatusChangedPayload;
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
                "Sözleşme durumu güncellendi: " + p.title(),
                html,
                event.eventId()
        );
    }
}
