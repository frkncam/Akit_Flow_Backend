package com.muhur.notification.consumer;

import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.SignatureRequestedPayload;
import com.muhur.notification.config.AppProperties;
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
public class SignatureRequestedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;
    private final AppProperties props;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_REQUESTED)
    public void onMessage(DomainEvent<SignatureRequestedPayload> event) {
        log.info("signature.requested received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        SignatureRequestedPayload p = event.payload();
        String signUrl = props.signBaseUrl() + "/" + p.token();

        String html = templates.render("contract-signature-requested", Map.of(
                "contractTitle", p.contractTitle(),
                "signerName", p.signerName(),
                "signUrl", signUrl,
                "expiresAt", p.expiresAt(),
                "contractId", p.contractId()
        ));

        emails.send(
                EmailType.CONTRACT_SIGNATURE_REQUESTED,
                p.signerEmail(),
                "Please sign: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
