package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.UserRegisteredPayload;
import com.muhur.notification.config.RabbitMQConfig;
import com.muhur.notification.domain.enums.EmailType;
import com.muhur.notification.service.EmailService;
import com.muhur.notification.service.IdempotencyService;
import com.muhur.notification.service.TemplateService;
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
                "Mühür'e hoş geldiniz!",
                html,
                event.eventId()
        );
    }
}
