package com.akitflow.workflow.service;

import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.workflow.dto.request.StartWorkflowRequest;
import com.akitflow.workflow.dto.response.WorkflowResponse;
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
