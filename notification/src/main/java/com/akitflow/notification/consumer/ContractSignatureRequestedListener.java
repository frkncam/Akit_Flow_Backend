package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.notification.config.AppProperties;
import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.event.payload.ContractSignatureRequestedPayload;
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
public class ContractSignatureRequestedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;
    private final AppProperties props;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_SIGNATURE_REQUESTED)
    public void onMessage(DomainEvent<ContractSignatureRequestedPayload> event) {
        log.info("contract.signature.requested alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        ContractSignatureRequestedPayload p = event.payload();
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
                "Lütfen imzalayın: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
