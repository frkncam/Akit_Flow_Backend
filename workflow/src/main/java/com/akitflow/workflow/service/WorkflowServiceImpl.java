package com.akitflow.workflow.service;

import com.akitflow.common.client.ContractClient;
import com.akitflow.common.client.dto.ContractDto;
import com.akitflow.common.event.payload.WorkflowApprovalRequestedPayload;
import com.akitflow.common.event.payload.WorkflowTransitionedPayload;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.workflow.domain.ApprovalStep;
import com.akitflow.workflow.domain.WorkflowInstance;
import com.akitflow.workflow.domain.enums.StepStatus;
import com.akitflow.workflow.domain.enums.WorkflowEvent;
import com.akitflow.workflow.domain.enums.WorkflowState;
import com.akitflow.workflow.dto.request.ApproverStepRequest;
import com.akitflow.workflow.dto.request.StartWorkflowRequest;
import com.akitflow.workflow.dto.response.ApprovalStepResponse;
import com.akitflow.workflow.dto.response.WorkflowResponse;
import com.akitflow.workflow.dto.response.WorkflowTransitionResponse;
import com.akitflow.workflow.event.publisher.WorkflowEventPublisher;
import com.akitflow.workflow.exception.WorkflowException;
import com.akitflow.workflow.mapper.WorkflowMapper;
import com.akitflow.workflow.repository.ApprovalStepRepository;
import com.akitflow.workflow.repository.WorkflowInstanceRepository;
import com.akitflow.workflow.repository.WorkflowTransitionRepository;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowStateMachineService stateMachineService;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowMapper mapper;
    private final ContractClient contractClient;

    @Override
    @Transactional
    public WorkflowResponse startWorkflow(Long contractId, StartWorkflowRequest request,
                                          HeaderPrincipal principal) {
        if (workflowInstanceRepository.existsByContractId(contractId)) {
            throw new WorkflowException("Bu sözleşme için zaten bir workflow mevcut.");
        }

        ContractDto contract = contractClient.getContract(
                contractId, principal.userId(), principal.organizationId(),
                principal.email(), principal.role());

        if (!contract.organizationId().equals(principal.organizationId())) {
            throw new ResourceNotFoundException("Sözleşme bulunamadı.");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setContractId(contractId);
        instance.setOrganizationId(principal.organizationId());
        instance.setCurrentState(WorkflowState.DRAFT);
        instance.setCreatedBy(principal.userId());
        instance = workflowInstanceRepository.save(instance);

        List<ApproverStepRequest> approvers = request.approvers();
        for (int i = 0; i < approvers.size(); i++) {
            ApproverStepRequest a = approvers.get(i);
            ApprovalStep step = new ApprovalStep();
            step.setWorkflowInstanceId(instance.getId());
            step.setOrderIndex(i);
            step.setApproverUserId(a.approverUserId());
            step.setApproverName(a.approverName());
            step.setApproverEmail(a.approverEmail());
            step.setStatus(StepStatus.PENDING);
            approvalStepRepository.save(step);
        }

        WorkflowState newState = stateMachineService.sendEvent(
                instance, WorkflowEvent.START_APPROVAL, principal.userId(), null);
        instance = workflowInstanceRepository.save(instance);

        eventPublisher.publishTransitioned(principal.organizationId(), principal.userId(),
                new WorkflowTransitionedPayload(contractId, WorkflowState.DRAFT.name(),
                        newState.name(), WorkflowEvent.START_APPROVAL.name()));

        ApprovalStep firstStep = approvalStepRepository
                .findFirstByWorkflowInstanceIdAndStatusOrderByOrderIndex(
                        instance.getId(), StepStatus.PENDING)
                .orElseThrow(() -> new WorkflowException("İlk onay adımı bulunamadı."));
        eventPublisher.publishApprovalRequested(principal.organizationId(), principal.userId(),
                new WorkflowApprovalRequestedPayload(
                        contractId, contract.title(),
                        firstStep.getApproverName(), firstStep.getApproverEmail(),
                        0, approvers.size()));

        return buildResponse(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowResponse> search(Predicate predicate, Pageable pageable, HeaderPrincipal principal) {
        return workflowInstanceRepository.findAll(predicate, pageable)
                .map(this::toSummaryResponse);
    }

    private WorkflowResponse toSummaryResponse(WorkflowInstance instance) {
        return new WorkflowResponse(
                instance.getId(),
                instance.getContractId(),
                instance.getCurrentState().name(),
                Collections.emptyList(),
                Collections.emptyList(),
                instance.getCreatedAt(),
                instance.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(Long contractId, HeaderPrincipal principal) {
        WorkflowInstance instance = workflowInstanceRepository
                .findByContractId(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow bulunamadı."));

        if (!instance.getOrganizationId().equals(principal.organizationId())) {
            throw new ResourceNotFoundException("Workflow bulunamadı.");
        }

        return buildResponse(instance);
    }

    @Override
    @Transactional
    public void cancelWorkflow(Long contractId, HeaderPrincipal principal) {
        WorkflowInstance instance = workflowInstanceRepository
                .findByContractId(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow bulunamadı."));

        if (!instance.getOrganizationId().equals(principal.organizationId())) {
            throw new ResourceNotFoundException("Workflow bulunamadı.");
        }

        WorkflowState oldState = instance.getCurrentState();
        WorkflowState newState = stateMachineService.sendEvent(
                instance, WorkflowEvent.CANCEL, principal.userId(), null);
        workflowInstanceRepository.save(instance);

        eventPublisher.publishTransitioned(principal.organizationId(), principal.userId(),
                new WorkflowTransitionedPayload(contractId, oldState.name(),
                        newState.name(), WorkflowEvent.CANCEL.name()));
    }

    private WorkflowResponse buildResponse(WorkflowInstance instance) {
        List<ApprovalStepResponse> steps = approvalStepRepository
                .findByWorkflowInstanceIdOrderByOrderIndex(instance.getId())
                .stream()
                .map(mapper::toStepResponse)
                .toList();

        List<WorkflowTransitionResponse> transitions = transitionRepository
                .findByWorkflowInstanceIdOrderByOccurredAtDesc(instance.getId())
                .stream()
                .map(mapper::toTransitionResponse)
                .toList();

        return new WorkflowResponse(
                instance.getId(),
                instance.getContractId(),
                instance.getCurrentState().name(),
                steps,
                transitions,
                instance.getCreatedAt(),
                instance.getUpdatedAt()
        );
    }
}
