package com.akitflow.notification.consumer;

import com.akitflow.common.event.DomainEvent;
import com.akitflow.notification.config.RabbitMQConfig;
import com.akitflow.notification.domain.enums.EmailType;
import com.akitflow.notification.event.payload.ContractSignedPayload;
import com.akitflow.notification.service.EmailService;
import com.akitflow.notification.service.IdempotencyService;
import com.akitflow.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractSignedListener {

    private final IdempotencyService idempotency;
    private final TemplateService templates;
    private final EmailService emails;

    @RabbitListener(queues = RabbitMQConfig.Q_CONTRACT_SIGNED)
    public void onMessage(DomainEvent<ContractSignedPayload> event) {
        log.info("contract.signed alındı: eventId={}", event.eventId());

        if (!idempotency.markIfNew(event.eventId(), event.eventType())) {
            log.info("Duplicate event atlandı: eventId={}", event.eventId());
            return;
        }

        ContractSignedPayload p = event.payload();

        // Recipient set: creator + tüm signer'lar (deduped)
        List<String> recipients = new ArrayList<>();
        if (p.creatorEmail() != null && !p.creatorEmail().isBlank()) {
            recipients.add(p.creatorEmail());
        }
        if (p.signerEmails() != null) {
            p.signerEmails().stream()
                    .filter(e -> e != null && !e.isBlank())
                    .filter(e -> !recipients.contains(e))
                    .forEach(recipients::add);
        }

        if (recipients.isEmpty()) {
            log.warn("contract.signed için alıcı yok, atlanıyor: contractId={}", p.contractId());
            return;
        }

        String html = templates.render("contract-signed", Map.of(
                "contractTitle", p.contractTitle(),
                "contractId", p.contractId(),
                "signerEmails", p.signerEmails() == null ? List.of() : p.signerEmails()
        ));

        for (String to : recipients) {
            emails.send(
                    EmailType.CONTRACT_SIGNED,
                    to,
                    "Sözleşme imzalandı: " + p.contractTitle(),
                    html,
                    event.eventId()
            );
        }
    }
}
