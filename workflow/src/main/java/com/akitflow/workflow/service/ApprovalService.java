package com.akitflow.workflow.service;

import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.workflow.dto.request.ApprovalDecisionRequest;
import com.akitflow.workflow.dto.response.PendingApprovalResponse;

import java.util.List;

public interface ApprovalService {

    void approveStep(Long stepId, ApprovalDecisionRequest request, HeaderPrincipal principal);

    void rejectStep(Long stepId, ApprovalDecisionRequest request, HeaderPrincipal principal);

    List<PendingApprovalResponse> myPendingApprovals(HeaderPrincipal principal);
}
