package com.muhur.common.scheduler;

import com.muhur.common.domain.FailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedEventRetryScheduler {

    private static final int TERMINAL_HOURS = 24;
    private static final int PAGE_SIZE = 500;

    private final FailedEventOps ops;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0 7 * * * *")
    public void retryFailedEvents() {
        Instant cutoff = Instant.now().minusSeconds(TERMINAL_HOURS * 3600L);
        int terminals = ops.markTerminalsOlderThan(cutoff);
        if (terminals > 0) {
            log.info("Batch-marked {} failed events as terminal (older than {}h)", terminals, TERMINAL_HOURS);
        }

        int totalSuccess = 0;
        int totalRetried = 0;
        int page = 0;

        List<FailedEvent> batch;
        do {
            batch = ops.loadFailedEvents(page, PAGE_SIZE);
            for (FailedEvent fe : batch) {
                try {
                    byte[] body = fe.getEventJson().getBytes(StandardCharsets.UTF_8);
                    MessageProperties props = new MessageProperties();
                    props.setContentType("application/json");
                    Message message = new Message(body, props);
                    rabbitTemplate.send(fe.getExchange(), fe.getRoutingKey(), message);

                    ops.deleteFailedEvent(fe.getId());
                    totalSuccess++;
                    log.info("Retried and removed failed event {}: exchange={} rk={}",
                            fe.getId(), fe.getExchange(), fe.getRoutingKey());
                } catch (Exception e) {
                    ops.updateRetryState(fe.getId(), e);
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
}
