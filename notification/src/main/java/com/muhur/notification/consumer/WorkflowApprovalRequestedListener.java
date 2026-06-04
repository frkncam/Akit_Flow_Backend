package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.WorkflowApprovalRequestedPayload;
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
