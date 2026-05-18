package com.akitflow.notification.consumer;

import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.event.ContractEventEnvelope;
import com.akitflow.notification.event.payload.ContractSignatureRejectedPayload;
import com.akitflow.notification.service.EmailService;
import com.akitflow.notification.service.IdempotencyService;
import com.akitflow.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractSignatureRejectedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_SIGNATURE_REJECTED)
    public void onMessage(ContractEventEnvelope<ContractSignatureRejectedPayload> event) {
        log.info("contract.signature.rejected alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        ContractSignatureRejectedPayload p = event.payload();
        if (p.creatorEmail() == null || p.creatorEmail().isBlank()) {
            log.warn("contract.signature.rejected için alıcı email yok, atlanıyor: contractId={}", p.contractId());
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("contractTitle", p.contractTitle());
        model.put("contractId", p.contractId());
        model.put("signerName", p.signerName());
        model.put("signerEmail", p.signerEmail());
        model.put("reason", p.reason() != null ? p.reason() : "(belirtilmedi)");

        String html = templates.render("contract-signature-rejected", model);

        emails.send(
                EmailType.CONTRACT_SIGNATURE_REJECTED,
                p.creatorEmail(),
                "Sözleşme reddedildi: " + p.contractTitle(),
                html,
                event.eventId()
        );
    }
}
