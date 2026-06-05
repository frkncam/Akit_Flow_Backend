package com.muhur.common.scheduler;

import com.muhur.common.domain.FailedEvent;
import com.muhur.common.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * {@link FailedEventRetryScheduler}'in transactional veritabani islemleri.
 * Ayri bir bean olarak tutulur cunku Spring'in proxy tabanli {@code @Transactional}
 * destegi ayni sinif icinden (self-invocation) yapilan cagrilarda devreye girmez;
 * scheduler bu metotlari proxy uzerinden cagirir.
 */
@Component
@RequiredArgsConstructor
public class FailedEventOps {

    private final FailedEventRepository failedEventRepository;

    @Transactional
    public int markTerminalsOlderThan(Instant cutoff) {
        return failedEventRepository.markTerminalFailuresOlderThan(cutoff);
    }

    @Transactional(readOnly = true)
    public List<FailedEvent> loadFailedEvents(int page, int pageSize) {
        return failedEventRepository.findByTerminalFailureFalseOrderByCreatedAtAsc(
                PageRequest.of(page, pageSize));
    }

    @Transactional
    public void deleteFailedEvent(Long id) {
        failedEventRepository.deleteById(id);
    }

    @Transactional
    public void updateRetryState(Long id, Exception e) {
        failedEventRepository.findById(id).ifPresent(fe -> {
            fe.setRetryCount(fe.getRetryCount() + 1);
            fe.setLastRetryAt(Instant.now());
            fe.setLastError(e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(1024, e.getMessage().length()))
                    : "Unknown");
            failedEventRepository.save(fe);
        });
    }
}
