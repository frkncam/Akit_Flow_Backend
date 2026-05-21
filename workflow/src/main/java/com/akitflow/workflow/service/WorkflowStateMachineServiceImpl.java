package com.akitflow.workflow.service;

import com.akitflow.workflow.domain.WorkflowInstance;
import com.akitflow.workflow.domain.enums.WorkflowEvent;
import com.akitflow.workflow.domain.enums.WorkflowState;
import com.akitflow.workflow.exception.WorkflowException;
import com.akitflow.workflow.statemachine.WorkflowStateMachineListener;
import com.akitflow.workflow.statemachine.guard.AllStepsApprovedGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStateMachineServiceImpl implements WorkflowStateMachineService {

    private final AllStepsApprovedGuard allStepsApprovedGuard;
    private final WorkflowStateMachineListener listener;

    @Override
    public WorkflowState sendEvent(WorkflowInstance instance, WorkflowEvent event,
                                   Long triggeredBy, String comment) {
        WorkflowState currentState = instance.getCurrentState();

        if (!currentState.canTransitionTo(event)) {
            throw new WorkflowException(
                    String.format("'%s' durumundaki workflow '%s' event'ini kabul etmez.", currentState, event));
        }

        if (event == WorkflowEvent.APPROVE && !allStepsApprovedGuard.evaluate(instance.getId())) {
            throw new WorkflowException("Tüm onay adımları tamamlanmadan workflow onaylanamaz.");
        }

        WorkflowState targetState = currentState.transition(event);
        instance.setCurrentState(targetState);

        listener.onTransition(instance.getId(), currentState, targetState, event, triggeredBy, comment);

        log.info("Workflow {} state changed: {} -> {}", instance.getId(), currentState, targetState);
        return targetState;
    }
}
