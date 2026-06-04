package com.muhur.audit.controller;

import com.muhur.audit.domain.enums.AggregateType;
import com.muhur.audit.dto.response.AuditEventResponse;
import com.muhur.audit.mapper.AuditEventMapper;
import com.muhur.audit.service.AuditService;
import com.muhur.common.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AuditController {

    private final AuditService auditService;
    private final AuditEventMapper mapper;

    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<Page<AuditEventResponse>> getContractAuditTrail(
            @AuthenticationPrincipal HeaderPrincipal principal,
            @PathVariable Long contractId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.getContractAuditTrail(principal.organizationId(), contractId, pageable)
                .map(mapper::toResponse));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<AuditEventResponse>> getUserActions(
            @AuthenticationPrincipal HeaderPrincipal principal,
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.getUserActions(principal.organizationId(), userId, from, to, pageable)
                .map(mapper::toResponse));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<AuditEventResponse> getByEventId(
            @AuthenticationPrincipal HeaderPrincipal principal,
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(mapper.toResponse(
                auditService.getByEventId(principal.organizationId(), eventId)));
    }

    @GetMapping
    public ResponseEntity<Page<AuditEventResponse>> search(
            @AuthenticationPrincipal HeaderPrincipal principal,
            @RequestParam(required = false) AggregateType aggregateType,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.search(
                        principal.organizationId(), aggregateType, eventType, from, to, pageable)
                .map(mapper::toResponse));
    }
}
