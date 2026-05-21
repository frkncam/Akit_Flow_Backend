package com.akitflow.workflow.service;

import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.workflow.dto.request.StartWorkflowRequest;
import com.akitflow.workflow.dto.response.WorkflowResponse;

public interface WorkflowService {

    WorkflowResponse startWorkflow(Long contractId, StartWorkflowRequest request,
                                   HeaderPrincipal principal);

    WorkflowResponse getWorkflow(Long contractId, HeaderPrincipal principal);

    void cancelWorkflow(Long contractId, HeaderPrincipal principal);
}
