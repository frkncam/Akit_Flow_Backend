package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.WorkflowApprovalRequestedPayload;
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
public class WorkflowApprovalRequestedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_WORKFLOW_APPROVAL_REQUESTED)
    public void onMessage(DomainEvent<WorkflowApprovalRequestedPayload> event) {
        log.info("workflow.approval.requested received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        WorkflowApprovalRequestedPayload p = event.payload();

        String html = templates.render("approval-requested", Map.of(
                "contractTitle", p.contractTitle(),
                "approverName", p.approverName(),
                "stepOrder", String.valueOf(p.stepOrder() + 1),
                "totalSteps", String.valueOf(p.totalSteps()),
                "contractId", String.valueOf(p.contractId())
        ));

        emails.send(
                EmailType.WORKFLOW_APPROVAL_REQUESTED,
                p.approverEmail(),
                "Onay Talebi: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
