package com.muhur.workflow.repository;

import com.muhur.workflow.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByWorkflowInstanceIdOrderByOccurredAtDesc(Long workflowInstanceId);
}
