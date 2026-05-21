package com.akitflow.workflow.controller;

import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.workflow.dto.request.ApprovalDecisionRequest;
import com.akitflow.workflow.dto.request.StartWorkflowRequest;
import com.akitflow.workflow.dto.response.PendingApprovalResponse;
import com.akitflow.workflow.dto.response.WorkflowResponse;
import com.akitflow.workflow.service.ApprovalService;
import com.akitflow.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final ApprovalService approvalService;

    @PostMapping("/contracts/{contractId}/start")
    public ResponseEntity<WorkflowResponse> startWorkflow(
            @PathVariable Long contractId,
            @Valid @RequestBody StartWorkflowRequest request,
            @AuthenticationPrincipal HeaderPrincipal principal) {
        WorkflowResponse response = workflowService.startWorkflow(contractId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<WorkflowResponse> getWorkflow(
            @PathVariable Long contractId,
            @AuthenticationPrincipal HeaderPrincipal principal) {
        return ResponseEntity.ok(workflowService.getWorkflow(contractId, principal));
    }

    @PostMapping("/steps/{stepId}/approve")
    public ResponseEntity<Void> approveStep(
            @PathVariable Long stepId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal HeaderPrincipal principal) {
        approvalService.approveStep(stepId, request, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/steps/{stepId}/reject")
    public ResponseEntity<Void> rejectStep(
            @PathVariable Long stepId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal HeaderPrincipal principal) {
        approvalService.rejectStep(stepId, request, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/contracts/{contractId}/cancel")
    public ResponseEntity<Void> cancelWorkflow(
            @PathVariable Long contractId,
            @AuthenticationPrincipal HeaderPrincipal principal) {
        workflowService.cancelWorkflow(contractId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-pending-approvals")
    public ResponseEntity<List<PendingApprovalResponse>> myPendingApprovals(
            @AuthenticationPrincipal HeaderPrincipal principal) {
        return ResponseEntity.ok(approvalService.myPendingApprovals(principal));
    }
}
