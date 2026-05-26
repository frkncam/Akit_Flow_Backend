package com.akitflow.audit.service;

import com.akitflow.audit.domain.AuditEvent;
import com.akitflow.audit.domain.enums.AggregateType;
import com.akitflow.audit.repository.AuditEventRepository;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Set<String> SENSITIVE_PAYLOAD_KEYS = Set.of("token", "inviteLink");

    private final AuditEventRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String INSERT_AUDIT = """
            INSERT INTO audit_schema.audit_events
            (event_id, event_type, aggregate_type, aggregate_id, organization_id, actor_id, payload, occurred_at)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7::jsonb, ?8)
            ON CONFLICT (event_id) DO NOTHING
            """;

    @Override
    @Transactional
    public void record(UUID eventId, String eventType, Instant occurredAt,
                       Long organizationId, Long actorId, JsonNode payload) {
        String prefix = eventType.split("\\.")[0];

        AggregateType aggregateType = switch (prefix) {
            case "user" -> AggregateType.USER;
            case "contract" -> AggregateType.CONTRACT;
            case "workflow" -> AggregateType.WORKFLOW;
            case "signature" -> AggregateType.SIGNATURE;
            default -> {
                log.warn("Unknown event prefix '{}' for eventType '{}', falling back to CONTRACT", prefix, eventType);
                yield AggregateType.CONTRACT;
            }
        };

        Long aggregateId = null;
        if (aggregateType != AggregateType.USER && payload.hasNonNull("contractId")) {
            aggregateId = payload.get("contractId").asLong();
        }

        JsonNode sanitized = redactSensitiveFields(payload);

        int updated = entityManager.createNativeQuery(INSERT_AUDIT)
                .setParameter(1, eventId)
                .setParameter(2, eventType)
                .setParameter(3, aggregateType.name())
                .setParameter(4, aggregateId)
                .setParameter(5, organizationId)
                .setParameter(6, actorId)
                .setParameter(7, sanitized.toString())
                .setParameter(8, occurredAt)
                .executeUpdate();

        if (updated > 0) {
            log.debug("Audit event recorded: eventId={} eventType={}", eventId, eventType);
        } else {
            log.debug("Duplicate audit event skipped: eventId={}", eventId);
        }
    }

    private static JsonNode redactSensitiveFields(JsonNode payload) {
        JsonNode copy = payload.deepCopy();
        if (copy instanceof ObjectNode objectNode) {
            objectNode.remove(SENSITIVE_PAYLOAD_KEYS);
        }
        return copy;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> getContractAuditTrail(Long organizationId, Long contractId, Pageable pageable) {
        return repository.findByOrganizationIdAndAggregateTypeAndAggregateIdOrderByRecordedAtDesc(
                organizationId, AggregateType.CONTRACT, contractId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> getUserActions(Long organizationId, Long userId, Instant from, Instant to, Pageable pageable) {
        Specification<AuditEvent> spec = (r, q, cb) -> cb.equal(r.get("organizationId"), organizationId);
        spec = spec.and((r, q, cb) -> cb.equal(r.get("actorId"), userId));
        if (from != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("recordedAt"), from));
        if (to != null)
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("recordedAt"), to));
        return repository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditEvent getByEventId(Long organizationId, UUID eventId) {
        return repository.findByEventIdAndOrganizationId(eventId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit event not found: " + eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> search(Long organizationId, AggregateType aggregateType,
                                   String eventType, Instant from, Instant to, Pageable pageable) {
        Specification<AuditEvent> spec = (r, q, cb) -> cb.equal(r.get("organizationId"), organizationId);
        if (aggregateType != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("aggregateType"), aggregateType));
        if (eventType != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("eventType"), eventType));
        if (from != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("recordedAt"), from));
        if (to != null)
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("recordedAt"), to));
        return repository.findAll(spec, pageable);
    }
}
