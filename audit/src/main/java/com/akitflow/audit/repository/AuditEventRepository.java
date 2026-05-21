package com.akitflow.audit.repository;

import com.akitflow.audit.domain.AuditEvent;
import com.akitflow.audit.domain.enums.AggregateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<AuditEvent> findByEventIdAndOrganizationId(UUID eventId, Long organizationId);

    Page<AuditEvent> findByOrganizationIdAndAggregateTypeAndAggregateIdOrderByRecordedAtDesc(
            Long organizationId, AggregateType aggregateType, Long aggregateId, Pageable pageable);

    @Query("""
        SELECT a FROM AuditEvent a
        WHERE a.organizationId = :orgId AND a.actorId = :actorId
          AND (:from IS NULL OR a.recordedAt >= :from)
          AND (:to   IS NULL OR a.recordedAt <= :to)
        ORDER BY a.recordedAt DESC
        """)
    Page<AuditEvent> findByActor(Long orgId, Long actorId, Instant from, Instant to, Pageable pageable);

    @Query("""
        SELECT a FROM AuditEvent a
        WHERE a.organizationId = :orgId
          AND (:aggregateType IS NULL OR a.aggregateType = :aggregateType)
          AND (:eventType IS NULL OR a.eventType = :eventType)
          AND (:from IS NULL OR a.recordedAt >= :from)
          AND (:to   IS NULL OR a.recordedAt <= :to)
        ORDER BY a.recordedAt DESC
        """)
    Page<AuditEvent> search(Long orgId, AggregateType aggregateType, String eventType,
                            Instant from, Instant to, Pageable pageable);
}
