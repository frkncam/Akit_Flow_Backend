package com.muhur.contract.event.listener;

import com.muhur.contract.config.RabbitMQConfig;
import com.muhur.contract.domain.ProcessedEvent;
import com.muhur.contract.domain.enums.ContractStatus;
import com.muhur.contract.repository.ContractRepository;
import com.muhur.contract.repository.ProcessedEventRepository;
import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.SignatureBatchRejectedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureBatchRejectedListener {

    private final ContractRepository contractRepository;
    private final ProcessedEventRepository processedEventRepository;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_BATCH_REJECTED)
    @Transactional
    public void onMessage(DomainEvent<SignatureBatchRejectedPayload> event) {
        log.info("signature.batch.rejected received: eventId={}", event.eventId());

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        SignatureBatchRejectedPayload p = event.payload();
        contractRepository.findById(p.contractId()).ifPresentOrElse(
                contract -> {
                    if (!contract.getStatus().canTransitionTo(ContractStatus.DRAFT)) {
                        log.warn("Skipping invalid transition {} -> DRAFT for contract {}",
                                contract.getStatus(), p.contractId());
                        return;
                    }
                    contract.transitionTo(ContractStatus.DRAFT);
                    contractRepository.save(contract);
                    log.info("Contract {} set to DRAFT after rejection", p.contractId());
                },
                () -> log.warn("Contract not found for batch rejected: contractId={}", p.contractId())
        );

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
