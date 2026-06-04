package com.muhur.workflow.service;

import com.muhur.common.security.HeaderPrincipal;
import com.muhur.workflow.dto.request.ApprovalDecisionRequest;
import com.muhur.workflow.dto.response.PendingApprovalResponse;

import java.util.List;

public interface ApprovalService {

    void approveStep(Long stepId, ApprovalDecisionRequest request, HeaderPrincipal principal);

    void rejectStep(Long stepId, ApprovalDecisionRequest request, HeaderPrincipal principal);

    List<PendingApprovalResponse> myPendingApprovals(HeaderPrincipal principal);
}
