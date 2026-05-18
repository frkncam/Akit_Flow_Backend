package com.akitflow.contract.event.payload;

import com.akitflow.contract.domain.enums.ContractStatus;

public record ContractStatusChangedPayload(
        Long contractId,
        String title,
        ContractStatus oldStatus,
        ContractStatus newStatus,
        String actorEmail
) {}
