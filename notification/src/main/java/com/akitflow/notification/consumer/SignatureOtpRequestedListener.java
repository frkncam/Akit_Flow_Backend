package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.SignatureOtpRequestedPayload;
import com.akitflow.notification.config.AppProperties;
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
public class SignatureOtpRequestedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;
    private final AppProperties props;

    @RabbitListener(queues = RabbitMQConfig.Q_SIGNATURE_OTP_REQUESTED)
    public void onMessage(DomainEvent<SignatureOtpRequestedPayload> event) {
        log.info("signature.otp.requested received: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event skipped: eventId={}", event.eventId());
            return;
        }

        SignatureOtpRequestedPayload p = event.payload();
        String signUrl = props.signBaseUrl() + "/" + p.token();

        String html = templates.render("contract-signature-otp", Map.of(
                "contractTitle", p.contractTitle(),
                "signerName", p.signerName(),
                "code", p.code(),
                "expiresAt", p.expiresAt(),
                "signUrl", signUrl
        ));

        emails.send(
                EmailType.CONTRACT_SIGNATURE_OTP,
                p.signerEmail(),
                "İmza doğrulama kodunuz",
                html,
                event.eventId()
        );
    }
}
