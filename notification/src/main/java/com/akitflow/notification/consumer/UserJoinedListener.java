package com.akitflow.notification.consumer;

import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.event.AuthEventEnvelope;
import com.akitflow.notification.event.payload.UserJoinedPayload;
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
public class UserJoinedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_USER_JOINED)
    public void onMessage(AuthEventEnvelope<UserJoinedPayload> event) {
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
