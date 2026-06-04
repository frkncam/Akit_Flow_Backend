package com.muhur.workflow.service;

import com.muhur.common.security.HeaderPrincipal;
import com.muhur.workflow.dto.request.StartWorkflowRequest;
import com.muhur.workflow.dto.response.WorkflowResponse;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkflowService {

    WorkflowResponse startWorkflow(Long contractId, StartWorkflowRequest request,
                                   HeaderPrincipal principal);

    WorkflowResponse getWorkflow(Long contractId, HeaderPrincipal principal);

    void cancelWorkflow(Long contractId, HeaderPrincipal principal);

    Page<WorkflowResponse> search(Predicate predicate, Pageable pageable, HeaderPrincipal principal);
}
