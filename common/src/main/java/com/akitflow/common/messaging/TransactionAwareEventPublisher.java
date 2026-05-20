package com.akitflow.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Component
@Slf4j
public class TransactionAwareEventPublisher {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final int MAX_ATTEMPTS = 3;

    private final RabbitTemplate rabbitTemplate;
    private final RetryTemplate retryTemplate;

    public TransactionAwareEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_ATTEMPTS);

        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setBackOffPolicy(backOff);
        this.retryTemplate.setRetryPolicy(retryPolicy);
    }

    public void publish(String exchange, String routingKey, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendWithRetry(exchange, routingKey, event);
                        }
                    });
        } else {
            sendWithRetry(exchange, routingKey, event);
        }
    }

    private void sendWithRetry(String exchange, String routingKey, Object event) {
        try {
            retryTemplate.execute((RetryCallback<Void, Exception>) ctx -> {
                if (ctx.getRetryCount() > 0) {
                    log.warn("Retrying event publish: exchange={} rk={} attempt={}",
                            exchange, routingKey, ctx.getRetryCount() + 1);
                }
                rabbitTemplate.convertAndSend(exchange, routingKey, event);
                return null;
            });
        } catch (Exception exhausted) {
            String eventJson = serializeQuietly(event);
            log.error("Event publish exhausted after {} retries: exchange={} rk={} eventType={} payload={}",
                    MAX_ATTEMPTS, exchange, routingKey,
                    event.getClass().getSimpleName(), eventJson, exhausted);
        }
    }

    private String serializeQuietly(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{\"serialization_error\":\"" + e.getMessage() + "\"}";
        }
    }
}
