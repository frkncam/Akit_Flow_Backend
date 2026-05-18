package com.akitflow.contract.domain.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ContractStatus {
    DRAFT,
    PENDING_SIGNATURE,
    ACTIVE,
    EXPIRED,
    TERMINATED;

    private static final Map<ContractStatus, Set<ContractStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(PENDING_SIGNATURE, TERMINATED),
            PENDING_SIGNATURE, EnumSet.of(ACTIVE, TERMINATED, DRAFT),
            ACTIVE, EnumSet.of(EXPIRED, TERMINATED),
            EXPIRED, EnumSet.of(TERMINATED),
            TERMINATED, EnumSet.noneOf(ContractStatus.class)
    );

    public boolean canTransitionTo(ContractStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
