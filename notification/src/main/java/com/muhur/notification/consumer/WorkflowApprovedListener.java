package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.WorkflowApprovedPayload;
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
public class WorkflowApprovedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_WORKFLOW_APPROVED)
    public void onMessage(DomainEvent<WorkflowApprovedPayload> event) {
        log.info("workflow.approved received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        WorkflowApprovedPayload p = event.payload();

        String html = templates.render("workflow-approved", Map.of(
                "contractTitle", p.contractTitle(),
                "contractId", String.valueOf(p.contractId())
        ));

        emails.send(
                EmailType.WORKFLOW_APPROVED,
                p.ownerEmail(),
                "Onay Tamamlandı: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
