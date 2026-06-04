package com.muhur.contract.dto.request;

import com.muhur.contract.domain.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusUpdateRequest(
        @NotNull ContractStatus status
) {}
