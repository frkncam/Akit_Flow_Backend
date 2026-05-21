package com.akitflow.contract.dto.response;

import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.domain.enums.ContractType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ContractResponse(
        Long id,
        Long organizationId,
        String title,
        String description,
        ContractType contractType,
        ContractStatus status,
        List<PartyResponse> parties,
        LocalDate startDate,
        LocalDate endDate,
        Instant signedAt,
        Instant terminatedAt,
        BigDecimal value,
        String currency,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt,
        String workflowState
) {}
