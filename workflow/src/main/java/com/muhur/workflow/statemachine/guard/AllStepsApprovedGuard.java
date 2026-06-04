package com.muhur.workflow.statemachine.guard;

import com.muhur.workflow.domain.enums.StepStatus;
import com.muhur.workflow.repository.ApprovalStepRepository;
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
