package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.UserInvitedPayload;
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
public class UserInvitedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_USER_INVITED)
    public void onMessage(DomainEvent<UserInvitedPayload> event) {
        log.info("user.invited alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        UserInvitedPayload p = event.payload();
        String html = templates.render("invite", Map.of(
                "organizationName", p.organizationName(),
                "invitedByName", p.invitedByName(),
                "inviteLink", p.inviteLink(),
                "role", p.role()
        ));

        emails.send(
                EmailType.INVITE,
                p.email(),
                p.organizationName() + " invited you to join",
                html,
                event.eventId()
        );
    }
}
