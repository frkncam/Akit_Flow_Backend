package com.akitflow.workflow.statemachine.guard;

import com.akitflow.workflow.domain.enums.StepStatus;
import com.akitflow.workflow.repository.ApprovalStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AllStepsApprovedGuard {

    private final ApprovalStepRepository approvalStepRepository;

    public boolean evaluate(Long workflowInstanceId) {
        return approvalStepRepository.findByWorkflowInstanceIdOrderByOrderIndex(workflowInstanceId)
                .stream()
                .allMatch(step -> step.getStatus() == StepStatus.APPROVED);
    }
}
