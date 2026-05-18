package com.akitflow.contract.dto.request;

import com.akitflow.contract.domain.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record ContractStatusUpdateRequest(
        @NotNull ContractStatus status
) {}
