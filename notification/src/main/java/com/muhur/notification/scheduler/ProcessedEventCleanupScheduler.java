package com.muhur.notification.scheduler;

import com.muhur.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventCleanupScheduler {

    private final ProcessedEventRepository processedEventRepository;

    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void cleanupProcessedEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(180));
        int deleted = processedEventRepository.deleteByProcessedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} processed event records older than 180 days", deleted);
        }
    }
}
