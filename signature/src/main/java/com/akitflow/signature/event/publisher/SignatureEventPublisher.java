package com.akitflow.signature.event.publisher;

import com.akitflow.signature.config.RabbitMQConfig;
import com.akitflow.signature.event.SignatureEvent;
import com.akitflow.signature.event.payload.SignatureBatchCompletedPayload;
import com.akitflow.signature.event.payload.SignatureBatchRejectedPayload;
import com.akitflow.signature.event.payload.SignatureRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignatureEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSignatureRequested(Long organizationId, Long actorId, SignatureRequestedPayload payload) {
        publish(RabbitMQConfig.RK_SIGNATURE_REQUESTED, organizationId, actorId, payload);
    }

    public void publishBatchCompleted(Long organizationId, Long actorId, SignatureBatchCompletedPayload payload) {
        publish(RabbitMQConfig.RK_SIGNATURE_BATCH_COMPLETED, organizationId, actorId, payload);
    }

    public void publishBatchRejected(Long organizationId, Long actorId, SignatureBatchRejectedPayload payload) {
        publish(RabbitMQConfig.RK_SIGNATURE_BATCH_REJECTED, organizationId, actorId, payload);
    }

    private <T> void publish(String routingKey, Long organizationId, Long actorId, T payload) {
        try {
            SignatureEvent<T> event = SignatureEvent.of(routingKey, organizationId, actorId, payload);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SIGNATURE_EXCHANGE, routingKey, event);
            log.debug("Event yayınlandı: {} eventId={}", routingKey, event.eventId());
        } catch (Exception e) {
            log.error("Event yayını başarısız ({}): {}", routingKey, e.getMessage(), e);
        }
    }
}
