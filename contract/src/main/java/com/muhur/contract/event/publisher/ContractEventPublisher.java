package com.muhur.contract.event.publisher;

import com.muhur.contract.config.RabbitMQConfig;
import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.ContractCreatedPayload;
import com.muhur.common.event.payload.ContractExpiringSoonPayload;
import com.muhur.common.event.payload.ContractStatusChangedPayload;
import com.muhur.common.messaging.TransactionAwareEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractEventPublisher {

    private final TransactionAwareEventPublisher txPublisher;

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
        DomainEvent<T> event = DomainEvent.of(routingKey, organizationId, actorId, payload);
        txPublisher.publish(RabbitMQConfig.CONTRACT_EXCHANGE, routingKey, event);
        log.debug("Event published: {} eventId={}", routingKey, event.eventId());
    }
}
