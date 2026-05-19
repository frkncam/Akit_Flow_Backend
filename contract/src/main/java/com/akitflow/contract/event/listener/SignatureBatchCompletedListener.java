package com.akitflow.contract.event.listener;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.event.SignatureEvent;
import com.akitflow.contract.event.payload.SignatureBatchCompletedPayload;
import com.akitflow.contract.repository.ContractRepository;
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

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_BATCH_COMPLETED)
    @Transactional
    public void onMessage(SignatureEvent<SignatureBatchCompletedPayload> event) {
        log.info("signature.batch.completed received: eventId={}", event.eventId());

        SignatureBatchCompletedPayload p = event.payload();
        contractRepository.findById(p.contractId()).ifPresentOrElse(
                contract -> {
                    contract.setStatus(ContractStatus.ACTIVE);
                    contract.setSignedAt(Instant.now());
                    contractRepository.save(contract);
                    log.info("Contract {} set to ACTIVE", p.contractId());
                },
                () -> log.warn("Contract not found for batch completed: contractId={}", p.contractId())
        );
    }
}
