package com.akitflow.workflow.domain.enums;

import java.util.EnumMap;
import java.util.Map;

public enum WorkflowState {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED;

    private static final Map<WorkflowState, Map<WorkflowEvent, WorkflowState>> TRANSITIONS =
            new EnumMap<>(WorkflowState.class);

    static {
        TRANSITIONS.put(DRAFT, Map.of(
                WorkflowEvent.START_APPROVAL, PENDING_APPROVAL,
                WorkflowEvent.CANCEL, CANCELLED
        ));
        TRANSITIONS.put(PENDING_APPROVAL, Map.of(
                WorkflowEvent.APPROVE, APPROVED,
                WorkflowEvent.REJECT, REJECTED,
                WorkflowEvent.CANCEL, CANCELLED
        ));
        TRANSITIONS.put(APPROVED, Map.of());
        TRANSITIONS.put(REJECTED, Map.of());
        TRANSITIONS.put(CANCELLED, Map.of());
    }

    public boolean canTransitionTo(WorkflowEvent event) {
        return TRANSITIONS.getOrDefault(this, Map.of()).containsKey(event);
    }

    public WorkflowState transition(WorkflowEvent event) {
        WorkflowState target = TRANSITIONS.getOrDefault(this, Map.of()).get(event);
        if (target == null) {
            throw new IllegalStateException(
                    String.format("'%s' durumu '%s' event'i için geçiş tanımı içermiyor.", this, event));
        }
        return target;
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
