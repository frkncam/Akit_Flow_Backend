package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.WorkflowApprovalRejectedPayload;
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
public class WorkflowApprovalRejectedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_WORKFLOW_APPROVAL_REJECTED)
    public void onMessage(DomainEvent<WorkflowApprovalRejectedPayload> event) {
        log.info("workflow.approval.rejected received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        WorkflowApprovalRejectedPayload p = event.payload();

        String html = templates.render("approval-rejected", Map.of(
                "contractTitle", p.contractTitle(),
                "rejectedByName", p.rejectedByName(),
                "reason", p.reason(),
                "contractId", String.valueOf(p.contractId())
        ));

        emails.send(
                EmailType.WORKFLOW_APPROVAL_REJECTED,
                p.ownerEmail(),
                "Onay Reddedildi: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
