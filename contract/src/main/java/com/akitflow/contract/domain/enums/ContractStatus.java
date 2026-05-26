package com.akitflow.contract.domain.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ContractStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    PENDING_SIGNATURE,
    ACTIVE,
    EXPIRED,
    TERMINATED;

    private static final Map<ContractStatus, Set<ContractStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(IN_REVIEW, PENDING_SIGNATURE, TERMINATED),
            IN_REVIEW, EnumSet.of(APPROVED, REJECTED, TERMINATED, DRAFT),
            APPROVED, EnumSet.of(PENDING_SIGNATURE, TERMINATED),
            REJECTED, EnumSet.of(DRAFT, TERMINATED),
            PENDING_SIGNATURE, EnumSet.of(ACTIVE, TERMINATED, DRAFT),
            ACTIVE, EnumSet.of(EXPIRED, TERMINATED),
            EXPIRED, EnumSet.of(TERMINATED),
            TERMINATED, EnumSet.noneOf(ContractStatus.class)
    );

    public boolean canTransitionTo(ContractStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
