package com.akitflow.contract.event.publisher;

import com.akitflow.contract.config.RabbitMQConfig;
import com.akitflow.contract.event.ContractEvent;
import com.akitflow.contract.event.payload.ContractCreatedPayload;
import com.akitflow.contract.event.payload.ContractExpiringSoonPayload;
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

    private <T> void publish(String routingKey, Long organizationId, Long actorId, T payload) {
        try {
            ContractEvent<T> event = ContractEvent.of(routingKey, organizationId, actorId, payload);
            rabbitTemplate.convertAndSend(RabbitMQConfig.CONTRACT_EXCHANGE, routingKey, event);
            log.debug("Event published: {} eventId={}", routingKey, event.eventId());
        } catch (Exception e) {
            log.error("Event publish failed ({}): {}", routingKey, e.getMessage(), e);
        }
    }
}
