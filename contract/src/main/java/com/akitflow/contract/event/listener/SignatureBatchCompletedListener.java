package com.akitflow.contract.event.listener;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.domain.ProcessedEvent;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ProcessedEventRepository;
import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.SignatureBatchCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureBatchCompletedListener {

    private final ContractRepository contractRepository;
    private final ProcessedEventRepository processedEventRepository;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_BATCH_COMPLETED)
    @Transactional
    public void onMessage(DomainEvent<SignatureBatchCompletedPayload> event) {
        log.info("signature.batch.completed received: eventId={}", event.eventId());

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        SignatureBatchCompletedPayload p = event.payload();
        contractRepository.findById(p.contractId()).ifPresentOrElse(
                contract -> {
                    if (!contract.getStatus().canTransitionTo(ContractStatus.ACTIVE)) {
                        log.warn("Skipping invalid transition {} -> ACTIVE for contract {}",
                                contract.getStatus(), p.contractId());
                        return;
                    }
                    contract.transitionTo(ContractStatus.ACTIVE);
                    contract.setSignedAt(Instant.now());
                    contractRepository.save(contract);
                    log.info("Contract {} set to ACTIVE", p.contractId());
                },
                () -> log.warn("Contract not found for batch completed: contractId={}", p.contractId())
        );

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
