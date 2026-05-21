package com.akitflow.contract.event.listener;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.domain.ProcessedEvent;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ProcessedEventRepository;
import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.WorkflowTransitionedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTransitionedListener {

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
        contractRepository.findById(p.contractId()).ifPresentOrElse(
                contract -> {
                    contract.setWorkflowState(p.toState());
                    contractRepository.save(contract);
                    log.info("Contract {} workflow_state set to {}", p.contractId(), p.toState());
                },
                () -> log.warn("Contract not found for workflow transition: contractId={}", p.contractId())
        );

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
