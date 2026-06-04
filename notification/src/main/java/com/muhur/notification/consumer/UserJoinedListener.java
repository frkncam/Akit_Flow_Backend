package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.UserJoinedPayload;
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
public class UserJoinedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_USER_JOINED)
    public void onMessage(DomainEvent<UserJoinedPayload> event) {
        log.info("user.joined alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        UserJoinedPayload p = event.payload();
        String html = templates.render("member-joined", Map.of(
                "firstName", p.firstName(),
                "lastName", p.lastName(),
                "organizationName", p.organizationName(),
                "role", p.role()
        ));

        // Şimdilik yeni katılan üyenin kendisine "hoş geldin" maili gönderiyoruz.
        // İleride owner/admin'lere bilgilendirme için ayrı bir akış kurulacak.
        emails.send(
                EmailType.MEMBER_JOINED,
                p.email(),
                p.organizationName() + " ekibine hoş geldiniz",
                html,
                event.eventId()
        );
    }
}
