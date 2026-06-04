package com.muhur.notification.service;

import com.muhur.notification.domain.ProcessedEvent;
import com.muhur.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final ProcessedEventRepository repository;

    @Override
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
