package com.akitflow.contract.event.publisher;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.event.ContractEvent;
import com.akitflow.contract.event.payload.ContractCreatedPayload;
import com.akitflow.contract.event.payload.ContractExpiringSoonPayload;
import com.akitflow.contract.event.payload.ContractSignatureRejectedPayload;
import com.akitflow.contract.event.payload.ContractSignatureRequestedPayload;
import com.akitflow.contract.event.payload.ContractSignedPayload;
import com.akitflow.contract.event.payload.ContractStatusChangedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishContractCreated(Long organizationId, Long actorId, ContractCreatedPayload payload) {
        publish(RabbitMQConfig.RK_CONTRACT_CREATED, organizationId, actorId, payload);
    }

    public void publishContractStatusChanged(Long organizationId, Long actorId, ContractStatusChangedPayload payload) {
        publish(RabbitMQConfig.RK_CONTRACT_STATUS_CHANGED, organizationId, actorId, payload);
    }

    public void publishContractExpiringSoon(Long organizationId, Long actorId, ContractExpiringSoonPayload payload) {
        publish(RabbitMQConfig.RK_CONTRACT_EXPIRING_SOON, organizationId, actorId, payload);
    }

    public void publishSignatureRequested(Long organizationId, Long actorId, ContractSignatureRequestedPayload payload) {
        publish(RabbitMQConfig.RK_CONTRACT_SIGNATURE_REQUESTED, organizationId, actorId, payload);
    }

    public void publishContractSigned(Long organizationId, Long actorId, ContractSignedPayload payload) {
        publish(RabbitMQConfig.RK_CONTRACT_SIGNED, organizationId, actorId, payload);
    }

    public void publishSignatureRejected(Long organizationId, Long actorId, ContractSignatureRejectedPayload payload) {
        publish(RabbitMQConfig.RK_CONTRACT_SIGNATURE_REJECTED, organizationId, actorId, payload);
    }

    private <T> void publish(String routingKey, Long organizationId, Long actorId, T payload) {
        try {
            ContractEvent<T> event = ContractEvent.of(routingKey, organizationId, actorId, payload);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CONTRACT_EXCHANGE, routingKey, event);
            log.debug("Event yayınlandı: {} eventId={}", routingKey, event.eventId());
        } catch (Exception e) {
            log.error("Event yayını başarısız ({}): {}", routingKey, e.getMessage(), e);
        }
    }
}
