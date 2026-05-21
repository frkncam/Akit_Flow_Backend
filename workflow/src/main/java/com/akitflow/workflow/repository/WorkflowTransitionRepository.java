package com.akitflow.workflow.repository;

import com.akitflow.workflow.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByWorkflowInstanceIdOrderByOccurredAtDesc(Long workflowInstanceId);
}
