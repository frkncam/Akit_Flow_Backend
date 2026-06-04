package com.muhur.workflow.repository;

import com.muhur.workflow.domain.ApprovalStep;
import com.muhur.workflow.domain.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByWorkflowInstanceIdOrderByOrderIndex(Long workflowInstanceId);

    Optional<ApprovalStep> findFirstByWorkflowInstanceIdAndStatusOrderByOrderIndex(
            Long workflowInstanceId, StepStatus status);

    List<ApprovalStep> findByApproverUserIdAndStatus(Long approverUserId, StepStatus status);

    List<ApprovalStep> findByWorkflowInstanceIdInOrderByWorkflowInstanceIdAscOrderIndexAsc(
            Collection<Long> workflowInstanceIds);
}
