package com.akitflow.workflow.event.publisher;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.*;
import com.akitflow.common.messaging.TransactionAwareEventPublisher;
import com.akitflow.workflow.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowEventPublisher {

    private final TransactionAwareEventPublisher publisher;

    public void publishTransitioned(Long organizationId, Long actorId,
                                    WorkflowTransitionedPayload payload) {
        publisher.publish(RabbitMQConfig.WORKFLOW_EXCHANGE,
                RabbitMQConfig.RK_WORKFLOW_TRANSITIONED,
                DomainEvent.of("workflow.transitioned", organizationId, actorId, payload));
    }

    public void publishApprovalRequested(Long organizationId, Long actorId,
                                         WorkflowApprovalRequestedPayload payload) {
        publisher.publish(RabbitMQConfig.WORKFLOW_EXCHANGE,
                RabbitMQConfig.RK_WORKFLOW_APPROVAL_REQUESTED,
                DomainEvent.of("workflow.approval.requested", organizationId, actorId, payload));
    }

    public void publishApprovalRejected(Long organizationId, Long actorId,
                                        WorkflowApprovalRejectedPayload payload) {
        publisher.publish(RabbitMQConfig.WORKFLOW_EXCHANGE,
                RabbitMQConfig.RK_WORKFLOW_APPROVAL_REJECTED,
                DomainEvent.of("workflow.approval.rejected", organizationId, actorId, payload));
    }

    public void publishApproved(Long organizationId, Long actorId,
                                WorkflowApprovedPayload payload) {
        publisher.publish(RabbitMQConfig.WORKFLOW_EXCHANGE,
                RabbitMQConfig.RK_WORKFLOW_APPROVED,
                DomainEvent.of("workflow.approved", organizationId, actorId, payload));
    }
}
