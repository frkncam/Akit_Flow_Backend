package com.muhur.workflow.service;

import com.muhur.common.client.ContractClient;
import com.muhur.common.client.dto.ContractDto;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.workflow.domain.ApprovalStep;
import com.muhur.workflow.domain.WorkflowInstance;
import com.muhur.workflow.domain.enums.StepStatus;
import com.muhur.workflow.domain.enums.WorkflowState;
import com.muhur.workflow.dto.request.ApprovalDecisionRequest;
import com.muhur.workflow.event.publisher.WorkflowEventPublisher;
import com.muhur.workflow.exception.InvalidApprovalActionException;
import com.muhur.workflow.repository.ApprovalStepRepository;
import com.muhur.workflow.repository.WorkflowInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalServiceImpl — validateStepAction guards")
class ApprovalServiceImplTest {

    @Mock
    private ApprovalStepRepository approvalStepRepository;
    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;
    @Mock
    private WorkflowStateMachineService stateMachineService;
    @Mock
    private WorkflowEventPublisher eventPublisher;
    @Mock
    private ContractClient contractClient;

    @InjectMocks
    private ApprovalServiceImpl approvalService;

    private static final HeaderPrincipal CORRECT_USER = new HeaderPrincipal(100L, 1L, "approver@test.com", "APPROVER");
    private static final HeaderPrincipal WRONG_USER = new HeaderPrincipal(999L, 1L, "wrong@test.com", "APPROVER");

    private WorkflowInstance instance;
    private ApprovalStep step;
    private ApprovalDecisionRequest request;

    @BeforeEach
    void setUp() {
        instance = new WorkflowInstance();
        instance.setId(10L);
        instance.setContractId(50L);
        instance.setOrganizationId(1L);
        instance.setCurrentState(WorkflowState.PENDING_APPROVAL);
        instance.setCreatedBy(1L);

        step = new ApprovalStep();
        step.setId(200L);
        step.setWorkflowInstanceId(10L);
        step.setOrderIndex(0);
        step.setApproverUserId(100L);
        step.setApproverName("Test Approver");
        step.setApproverEmail("approver@test.com");
        step.setStatus(StepStatus.PENDING);

        request = new ApprovalDecisionRequest("Approved via test");

        lenient().when(approvalStepRepository.findFirstByWorkflowInstanceIdAndStatusOrderByOrderIndex(
                10L, StepStatus.PENDING)).thenReturn(Optional.of(step));
    }

    // ── Wrong user ───────────────────────────────────────────────

    @Test
    @DisplayName("wrong user approval → InvalidApprovalActionException")
    void wrongUserApprovalThrowsForbidden() {
        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> approvalService.approveStep(200L, request, WRONG_USER))
                .isInstanceOf(InvalidApprovalActionException.class)
                .hasMessageContaining("yetkili onaylayıcı");

        verify(approvalStepRepository, never()).save(any());
    }

    @Test
    @DisplayName("wrong user reject → InvalidApprovalActionException")
    void wrongUserRejectThrowsForbidden() {
        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> approvalService.rejectStep(200L, request, WRONG_USER))
                .isInstanceOf(InvalidApprovalActionException.class)
                .hasMessageContaining("yetkili onaylayıcı");

        verify(approvalStepRepository, never()).save(any());
    }

    // ── Out-of-order step ────────────────────────────────────────

    @Test
    @DisplayName("out-of-order step approval → rejected")
    void outOfOrderStepIsRejected() {
        ApprovalStep firstPendingStep = new ApprovalStep();
        firstPendingStep.setId(201L);
        firstPendingStep.setWorkflowInstanceId(10L);
        firstPendingStep.setOrderIndex(0);
        firstPendingStep.setApproverUserId(100L);
        firstPendingStep.setStatus(StepStatus.PENDING);

        step.setOrderIndex(1); // this is step #2, but step #1 is still pending

        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));
        when(approvalStepRepository.findFirstByWorkflowInstanceIdAndStatusOrderByOrderIndex(10L, StepStatus.PENDING))
                .thenReturn(Optional.of(firstPendingStep));

        assertThatThrownBy(() -> approvalService.approveStep(200L, request, CORRECT_USER))
                .isInstanceOf(InvalidApprovalActionException.class)
                .hasMessageContaining("Sıradaki adım");

        verify(approvalStepRepository, never()).save(any());
    }

    // ── Already decided ──────────────────────────────────────────

    @Test
    @DisplayName("re-approving an already approved step → rejected")
    void alreadyApprovedStepIsRejected() {
        step.setStatus(StepStatus.APPROVED);

        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> approvalService.approveStep(200L, request, CORRECT_USER))
                .isInstanceOf(InvalidApprovalActionException.class)
                .hasMessageContaining("zaten karara bağlanmış");

        verify(approvalStepRepository, never()).save(any());
    }

    @Test
    @DisplayName("re-rejecting an already rejected step → rejected")
    void alreadyRejectedStepIsRejected() {
        step.setStatus(StepStatus.REJECTED);

        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> approvalService.rejectStep(200L, request, CORRECT_USER))
                .isInstanceOf(InvalidApprovalActionException.class)
                .hasMessageContaining("zaten karara bağlanmış");

        verify(approvalStepRepository, never()).save(any());
    }

    // ── Wrong workflow state ─────────────────────────────────────

    @Test
    @DisplayName("approval when workflow not in PENDING_APPROVAL → rejected")
    void wrongWorkflowStateIsRejected() {
        instance.setCurrentState(WorkflowState.DRAFT);

        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> approvalService.approveStep(200L, request, CORRECT_USER))
                .isInstanceOf(InvalidApprovalActionException.class)
                .hasMessageContaining("onay bekleyen durumda değil");

        verify(approvalStepRepository, never()).save(any());
    }

    // ── Happy path (approve) ─────────────────────────────────────

    @Test
    @DisplayName("happy path approve → step saved, event published when all approved")
    void happyPathApprovalSavesStepAndPublishesEvent() {
        when(approvalStepRepository.findById(200L)).thenReturn(Optional.of(step));
        when(workflowInstanceRepository.findById(10L)).thenReturn(Optional.of(instance));
        when(approvalStepRepository.findFirstByWorkflowInstanceIdAndStatusOrderByOrderIndex(10L, StepStatus.PENDING))
                .thenReturn(Optional.of(step));
        when(approvalStepRepository.findByWorkflowInstanceIdOrderByOrderIndex(10L))
                .thenReturn(List.of(step));
        when(stateMachineService.sendEvent(any(), any(), any(), any()))
                .thenReturn(WorkflowState.APPROVED);
        lenient().when(contractClient.getContract(any(), any(), any(), any(), any()))
                .thenReturn(new ContractDto(50L, "Test Contract", "DRAFT", 1L, 1L, "creator@test.com", java.time.Instant.now()));

        approvalService.approveStep(200L, request, CORRECT_USER);

        verify(approvalStepRepository).save(step);
    }
}
