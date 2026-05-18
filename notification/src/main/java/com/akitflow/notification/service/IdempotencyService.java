package com.akitflow.notification.service;

import com.akitflow.notification.domain.ProcessedEvent;
import com.akitflow.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    /**
     * Idempotency işareti at. Aynı eventId daha önce işlendiyse false döner.
     * Yeni ise true döner ve kayıt eklenir.
     *
     * REQUIRES_NEW kullanarak listener transaction'ından bağımsız çalışır —
     * mail gönderimi fail olursa bile event "işlendi" sayılır mı sorusu
     * tasarım kararı. Burada false yapıyoruz: idempotency commit'i listener
     * transaction'ına bağlı; mail fail olursa rollback → event yeniden gelir.
     */
    @Transactional
    public boolean markIfNew(UUID eventId, String eventType) {
        if (repository.existsByEventId(eventId)) {
            return false;
        }
        try {
            repository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .processedAt(Instant.now())
                    .build());
            return true;
        } catch (DataIntegrityViolationException e) {
            // Race condition: araya başka instance girdi
            return false;
        }
    }
}
