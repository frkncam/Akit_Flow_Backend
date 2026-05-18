package com.akitflow.contract.dto.request;

import com.akitflow.contract.domain.enums.ContractType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractUpdateRequest(
        @Size(max = 255) String title,
        String description,
        ContractType contractType,
        @Valid List<PartyRequest> parties,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal value,
        @Size(max = 3) String currency
) {}
