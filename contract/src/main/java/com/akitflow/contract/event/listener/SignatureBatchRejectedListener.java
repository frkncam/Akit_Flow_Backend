package com.akitflow.contract.event.listener;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.event.SignatureEvent;
import com.akitflow.contract.event.payload.SignatureBatchRejectedPayload;
import com.akitflow.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureBatchRejectedListener {

    private final ContractRepository contractRepository;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_BATCH_REJECTED)
    @Transactional
    public void onMessage(SignatureEvent<SignatureBatchRejectedPayload> event) {
        log.info("signature.batch.rejected received: eventId={}", event.eventId());

        SignatureBatchRejectedPayload p = event.payload();
        contractRepository.findById(p.contractId()).ifPresentOrElse(
                contract -> {
                    contract.setStatus(ContractStatus.DRAFT);
                    contractRepository.save(contract);
                    log.info("Contract {} set to DRAFT after rejection", p.contractId());
                },
                () -> log.warn("Contract not found for batch rejected: contractId={}", p.contractId())
        );
    }
}
