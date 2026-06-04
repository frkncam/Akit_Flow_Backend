package com.muhur.audit.repository;

import com.muhur.audit.domain.AuditEvent;
import com.muhur.audit.domain.enums.AggregateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>,
        JpaSpecificationExecutor<AuditEvent> {

    boolean existsByEventId(UUID eventId);

    Optional<AuditEvent> findByEventIdAndOrganizationId(UUID eventId, Long organizationId);

    Page<AuditEvent> findByOrganizationIdAndAggregateTypeAndAggregateIdOrderByRecordedAtDesc(
            Long organizationId, AggregateType aggregateType, Long aggregateId, Pageable pageable);
}
