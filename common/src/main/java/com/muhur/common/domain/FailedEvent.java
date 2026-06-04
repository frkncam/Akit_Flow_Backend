package com.muhur.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "failed_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_json", nullable = false, columnDefinition = "TEXT")
    private String eventJson;

    @Column(nullable = false, length = 128)
    private String exchange;

    @Column(name = "routing_key", nullable = false, length = 128)
    private String routingKey;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "terminal_failure", nullable = false)
    @Builder.Default
    private boolean terminalFailure = false;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;
}
