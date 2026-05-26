package com.akitflow.common.scheduler;

import com.akitflow.common.domain.FailedEvent;
import com.akitflow.common.repository.FailedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedEventRetryScheduler {

    private static final int TERMINAL_HOURS = 24;
    private static final int PAGE_SIZE = 500;

    private final FailedEventRepository failedEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0 7 * * * *")
    public void retryFailedEvents() {
        int terminals = batchMarkTerminals();
        if (terminals > 0) {
            log.info("Batch-marked {} failed events as terminal (older than {}h)", terminals, TERMINAL_HOURS);
        }

        int totalSuccess = 0;
        int totalRetried = 0;
        int page = 0;

        List<FailedEvent> batch;
        do {
            batch = loadFailedEvents(page);
            for (FailedEvent fe : batch) {
                try {
                    byte[] body = fe.getEventJson().getBytes(StandardCharsets.UTF_8);
                    MessageProperties props = new MessageProperties();
                    props.setContentType("application/json");
                    Message message = new Message(body, props);
                    rabbitTemplate.send(fe.getExchange(), fe.getRoutingKey(), message);

                    deleteFailedEvent(fe.getId());
                    totalSuccess++;
                    log.info("Retried and removed failed event {}: exchange={} rk={}",
                            fe.getId(), fe.getExchange(), fe.getRoutingKey());
                } catch (Exception e) {
                    updateRetryState(fe.getId(), e);
                    totalRetried++;
                    log.warn("Retry failed for event {} (attempt {}): exchange={} rk={} error={}",
                            fe.getId(), fe.getRetryCount() + 1, fe.getExchange(), fe.getRoutingKey(), e.getMessage());
                }
            }
            page++;
        } while (batch.size() == PAGE_SIZE);

        if (totalSuccess > 0 || totalRetried > 0 || terminals > 0) {
            log.info("Failed event retry cycle: success={} retried={} terminal={}", totalSuccess, totalRetried, terminals);
        }
    }

    @Transactional
    public int batchMarkTerminals() {
        Instant cutoff = Instant.now().minusSeconds(TERMINAL_HOURS * 3600L);
        return failedEventRepository.markTerminalFailuresOlderThan(cutoff);
    }

    @Transactional(readOnly = true)
    public List<FailedEvent> loadFailedEvents(int page) {
        return failedEventRepository.findByTerminalFailureFalseOrderByCreatedAtAsc(
                PageRequest.of(page, PAGE_SIZE));
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
