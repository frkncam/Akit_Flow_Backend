package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.UserRegisteredPayload;
import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.service.EmailService;
import com.akitflow.notification.service.IdempotencyService;
import com.akitflow.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_USER_REGISTERED)
    public void onMessage(DomainEvent<UserRegisteredPayload> event) {
        log.info("user.registered alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        UserRegisteredPayload p = event.payload();
        String html = templates.render("welcome", Map.of(
                "firstName", p.firstName(),
                "lastName", p.lastName(),
                "organizationName", p.organizationName()
        ));

        emails.send(
                EmailType.WELCOME,
                p.email(),
                "AkitFlow'a hoş geldiniz!",
                html,
                event.eventId()
        );
    }
}
