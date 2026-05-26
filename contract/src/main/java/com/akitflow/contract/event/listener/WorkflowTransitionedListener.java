package com.akitflow.contract.event.listener;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ProcessedEvent;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ProcessedEventRepository;
import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.WorkflowTransitionedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTransitionedListener {

    private static final Map<String, ContractStatus> WORKFLOW_TO_CONTRACT_STATUS = Map.of(
            "PENDING_APPROVAL", ContractStatus.IN_REVIEW,
            "APPROVED", ContractStatus.APPROVED,
            "REJECTED", ContractStatus.REJECTED,
            "CANCELLED", ContractStatus.DRAFT
    );

    private final ContractRepository contractRepository;
    private final ProcessedEventRepository processedEventRepository;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_WORKFLOW_TRANSITIONED)
    @Transactional
    public void onMessage(DomainEvent<WorkflowTransitionedPayload> event) {
        log.info("workflow.transitioned received: eventId={}", event.eventId());

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        WorkflowTransitionedPayload p = event.payload();
        ContractStatus targetStatus = WORKFLOW_TO_CONTRACT_STATUS.get(p.toState());
        if (targetStatus == null) {
            log.debug("No contract status mapping for workflow state '{}', skipping", p.toState());
            saveProcessedEventSafely(event.eventId());
            return;
        }

        contractRepository.findById(p.contractId()).ifPresentOrElse(
                contract -> {
                    if (!contract.getStatus().canTransitionTo(targetStatus)) {
                        log.warn("Skipping invalid transition {} -> {} for contract {}",
                                contract.getStatus(), targetStatus, p.contractId());
                        return;
                    }
                    contract.transitionTo(targetStatus);
                    contractRepository.save(contract);
                    log.info("Contract {} status transitioned to {} (workflow: {} -> {})",
                            p.contractId(), targetStatus, p.fromState(), p.toState());
                },
                () -> log.warn("Contract not found for workflow transition: contractId={}", p.contractId())
        );

        saveProcessedEventSafely(event.eventId());
    }

    private void saveProcessedEventSafely(UUID eventId) {
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate event already processed by concurrent listener: eventId={}", eventId);
        }
    }
}
