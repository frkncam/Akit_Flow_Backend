package com.muhur.workflow.repository;

import com.muhur.workflow.domain.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long>, QuerydslPredicateExecutor<WorkflowInstance> {

    Optional<WorkflowInstance> findByContractId(Long contractId);

    boolean existsByContractId(Long contractId);
}
