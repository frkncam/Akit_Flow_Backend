package com.akitflow.contract.scheduler;

import com.akitflow.contract.config.AppProperties;
import com.akitflow.contract.domain.Contract;
import com.akitflow.common.event.payload.ContractExpiringSoonPayload;
import com.akitflow.contract.event.publisher.ContractEventPublisher;
import com.akitflow.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiringContractScheduler {

    private static final int[] THRESHOLDS_ASC = { 1, 7, 30 };

    private final ContractRepository contractRepository;
    private final ContractEventPublisher eventPublisher;
    private final AppProperties props;

    @Scheduled(cron = "${app.scheduler.contract-expiring-cron}",
               zone = "${app.scheduler.contract-expiring-zone}")
    @Transactional
    public void notifyExpiring() {
        LocalDate today = LocalDate.now(ZoneId.of(props.scheduler().contractExpiringZone()));
        LocalDate horizon = today.plusDays(30);

        List<Contract> expiring = contractRepository.findExpiringActive(today, horizon);
        int emittedCount = 0;

        for (Contract c : expiring) {
            long daysLeft = ChronoUnit.DAYS.between(today, c.getEndDate());
            for (int threshold : THRESHOLDS_ASC) {
                if (daysLeft <= threshold && isNotYetNotified(c, threshold)) {
                    eventPublisher.publishContractExpiringSoon(
                            c.getOrganizationId(),
                            c.getCreatedBy(),
                            new ContractExpiringSoonPayload(
                                    c.getId(),
                                    c.getTitle(),
                                    c.getEndDate(),
                                    threshold,
                                    c.getCreatorEmail()
                            )
                    );
                    markNotified(c, threshold);
                    emittedCount++;
                    break; // only fire the closest threshold this run
                }
            }
        }

        if (!expiring.isEmpty()) {
            contractRepository.saveAll(expiring);
        }
        log.info("Expiring scan: total={}, emitted={}", expiring.size(), emittedCount);
    }

    private boolean isNotYetNotified(Contract c, int threshold) {
        return switch (threshold) {
            case 1 -> c.getNotified1dAt() == null;
            case 7 -> c.getNotified7dAt() == null;
            case 30 -> c.getNotified30dAt() == null;
            default -> true;
        };
    }

    private void markNotified(Contract c, int threshold) {
        Instant now = Instant.now();
        switch (threshold) {
            case 1 -> c.setNotified1dAt(now);
            case 7 -> c.setNotified7dAt(now);
            case 30 -> c.setNotified30dAt(now);
        }
    }
}
