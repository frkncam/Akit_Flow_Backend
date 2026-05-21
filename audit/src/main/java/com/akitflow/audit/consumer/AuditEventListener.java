package com.akitflow.audit.consumer;

import com.akitflow.audit.config.RabbitMQConfig;
import com.akitflow.audit.service.AuditService;
import com.akitflow.common.event.DomainEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final TypeReference<DomainEvent<JsonNode>> EVENT_TYPE =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @RabbitListener(queues = RabbitMQConfig.Q_AUDIT_EVENTS)
    public void onMessage(Message message) throws IOException {
        DomainEvent<JsonNode> event = objectMapper.readValue(message.getBody(), EVENT_TYPE);
        auditService.record(event.eventId(), event.eventType(), event.occurredAt(),
                event.organizationId(), event.actorId(), event.payload());
    }
}
