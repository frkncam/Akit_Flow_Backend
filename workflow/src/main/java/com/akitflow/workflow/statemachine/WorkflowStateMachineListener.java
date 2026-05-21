package com.akitflow.workflow.statemachine;

import com.akitflow.workflow.domain.WorkflowTransition;
import com.akitflow.workflow.domain.enums.WorkflowState;
import com.akitflow.workflow.domain.enums.WorkflowEvent;
import com.akitflow.workflow.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowStateMachineListener {

    private final WorkflowTransitionRepository transitionRepository;

    public void onTransition(Long workflowInstanceId, WorkflowState fromState,
                             WorkflowState toState, WorkflowEvent event,
                             Long triggeredBy, String comment) {
        WorkflowTransition transition = new WorkflowTransition();
        transition.setWorkflowInstanceId(workflowInstanceId);
        transition.setFromState(fromState != null ? fromState.name() : null);
        transition.setToState(toState.name());
        transition.setEvent(event.name());
        transition.setTriggeredBy(triggeredBy);
        transition.setComment(comment);
        transitionRepository.save(transition);

        log.info("Workflow {} transitioned: {} --[{}]--> {}",
                workflowInstanceId, fromState, event, toState);
    }
}
