package com.muhur.audit.service;

import com.muhur.audit.domain.AuditEvent;
import com.muhur.audit.domain.enums.AggregateType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AuditService {

    void record(UUID eventId, String eventType, Instant occurredAt,
                Long organizationId, Long actorId, JsonNode payload);

    Page<AuditEvent> getContractAuditTrail(Long organizationId, Long contractId, Pageable pageable);

    Page<AuditEvent> getUserActions(Long organizationId, Long userId, Instant from, Instant to, Pageable pageable);

    AuditEvent getByEventId(Long organizationId, UUID eventId);

    Page<AuditEvent> search(Long organizationId, AggregateType aggregateType,
                            String eventType, Instant from, Instant to, Pageable pageable);
}
