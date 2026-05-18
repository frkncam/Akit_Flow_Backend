package com.akitflow.notification.consumer;

import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.event.AuthEventEnvelope;
import com.akitflow.notification.event.payload.UserInvitedPayload;
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
public class UserInvitedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_USER_INVITED)
    public void onMessage(AuthEventEnvelope<UserInvitedPayload> event) {
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
                p.organizationName() + " sizi davet etti",
                html,
                event.eventId()
        );
    }
}
