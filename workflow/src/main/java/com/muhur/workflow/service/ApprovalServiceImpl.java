package com.muhur.workflow.service;

import com.muhur.common.client.ContractClient;
import com.muhur.common.client.dto.ContractDto;
import com.muhur.common.event.payload.*;
import com.muhur.common.exception.ResourceNotFoundException;
import com.muhur.common.security.HeaderPrincipal;
import com.muhur.workflow.domain.ApprovalStep;
import com.muhur.workflow.domain.WorkflowInstance;
import com.muhur.workflow.domain.enums.StepStatus;
import com.muhur.workflow.domain.enums.WorkflowEvent;
import com.muhur.workflow.domain.enums.WorkflowState;
import com.muhur.workflow.dto.request.ApprovalDecisionRequest;
import com.muhur.workflow.dto.response.PendingApprovalResponse;
import com.muhur.workflow.event.publisher.WorkflowEventPublisher;
import com.muhur.workflow.exception.InvalidApprovalActionException;
import com.muhur.workflow.exception.WorkflowException;
import com.muhur.workflow.repository.ApprovalStepRepository;
import com.muhur.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalStepRepository approvalStepRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStateMachineService stateMachineService;
    private final WorkflowEventPublisher eventPublisher;
    private final ContractClient contractClient;

    @Override
    @Transactional
    public void approveStep(Long stepId, ApprovalDecisionRequest request,
                            HeaderPrincipal principal) {
        ApprovalStep step = approvalStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Onay adımı bulunamadı."));

        WorkflowInstance instance = workflowInstanceRepository.findById(step.getWorkflowInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow bulunamadı."));

        validateStepAction(step, instance, principal);

        step.setStatus(StepStatus.APPROVED);
        step.setComment(request.comment());
        step.setDecidedAt(Instant.now());
        approvalStepRepository.save(step);

        List<ApprovalStep> allSteps = approvalStepRepository
                .findByWorkflowInstanceIdOrderByOrderIndex(instance.getId());
        boolean allApproved = allSteps.stream().allMatch(s -> s.getStatus() == StepStatus.APPROVED);

        if (allApproved) {
            WorkflowState oldState = instance.getCurrentState();
            WorkflowState newState = stateMachineService.sendEvent(instance,
                    WorkflowEvent.APPROVE, principal.userId(), null);
            workflowInstanceRepository.save(instance);

            eventPublisher.publishTransitioned(principal.organizationId(), principal.userId(),
                    new WorkflowTransitionedPayload(instance.getContractId(),
                            oldState.name(), newState.name(),
                            WorkflowEvent.APPROVE.name()));

            ContractDto contract = contractClient.getContract(
                    instance.getContractId(), principal.userId(),
                    principal.organizationId(), principal.email(), principal.role());

            eventPublisher.publishApproved(principal.organizationId(), principal.userId(),
                    new WorkflowApprovedPayload(
                            instance.getContractId(), contract.title(), contract.creatorEmail()));
        } else {
            ApprovalStep nextStep = allSteps.stream()
                    .filter(s -> s.getStatus() == StepStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new WorkflowException("Sıradaki onay adımı bulunamadı."));

            ContractDto contract = contractClient.getContract(
                    instance.getContractId(), principal.userId(),
                    principal.organizationId(), principal.email(), principal.role());

            eventPublisher.publishApprovalRequested(principal.organizationId(), principal.userId(),
                    new WorkflowApprovalRequestedPayload(
                            instance.getContractId(), contract.title(),
                            nextStep.getApproverName(), nextStep.getApproverEmail(),
                            nextStep.getOrderIndex(), allSteps.size()));
        }
    }

    @Override
    @Transactional
    public void rejectStep(Long stepId, ApprovalDecisionRequest request,
                           HeaderPrincipal principal) {
        ApprovalStep step = approvalStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Onay adımı bulunamadı."));

        WorkflowInstance instance = workflowInstanceRepository.findById(step.getWorkflowInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow bulunamadı."));

        validateStepAction(step, instance, principal);

        step.setStatus(StepStatus.REJECTED);
        step.setComment(request.comment());
        step.setDecidedAt(Instant.now());
        approvalStepRepository.save(step);

        WorkflowState oldState = instance.getCurrentState();
        WorkflowState newState = stateMachineService.sendEvent(instance,
                WorkflowEvent.REJECT, principal.userId(), request.comment());
        workflowInstanceRepository.save(instance);

        eventPublisher.publishTransitioned(principal.organizationId(), principal.userId(),
                new WorkflowTransitionedPayload(instance.getContractId(),
                        oldState.name(), newState.name(),
                        WorkflowEvent.REJECT.name()));

        ContractDto contract = contractClient.getContract(
                instance.getContractId(), principal.userId(),
                principal.organizationId(), principal.email(), principal.role());

        eventPublisher.publishApprovalRejected(principal.organizationId(), principal.userId(),
                new WorkflowApprovalRejectedPayload(
                        instance.getContractId(), contract.title(),
                        contract.creatorEmail(), step.getApproverName(),
                        request.comment() != null ? request.comment() : ""));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingApprovalResponse> myPendingApprovals(HeaderPrincipal principal) {
        List<ApprovalStep> pendingSteps = approvalStepRepository
                .findByApproverUserIdAndStatus(principal.userId(), StepStatus.PENDING);

        if (pendingSteps.isEmpty()) {
            return List.of();
        }

        Set<Long> instanceIds = pendingSteps.stream()
                .map(ApprovalStep::getWorkflowInstanceId)
                .collect(Collectors.toSet());

        Map<Long, WorkflowInstance> instancesById = workflowInstanceRepository
                .findAllById(instanceIds).stream()
                .collect(Collectors.toMap(WorkflowInstance::getId, Function.identity()));

        Map<Long, List<ApprovalStep>> stepsByInstanceId = approvalStepRepository
                .findByWorkflowInstanceIdInOrderByWorkflowInstanceIdAscOrderIndexAsc(instanceIds)
                .stream()
                .collect(Collectors.groupingBy(ApprovalStep::getWorkflowInstanceId));

        return pendingSteps.stream()
                .filter(step -> {
                    WorkflowInstance instance = instancesById.get(step.getWorkflowInstanceId());
                    if (instance == null
                            || instance.getCurrentState() != WorkflowState.PENDING_APPROVAL) {
                        return false;
                    }
                    ApprovalStep currentStep = firstPendingStep(
                            stepsByInstanceId.get(instance.getId()));
                    return currentStep != null && currentStep.getId().equals(step.getId());
                })
                .map(step -> {
                    WorkflowInstance instance = instancesById.get(step.getWorkflowInstanceId());
                    List<ApprovalStep> allSteps = stepsByInstanceId.get(instance.getId());
                    String title = fetchContractTitle(instance.getContractId(), principal);
                    return new PendingApprovalResponse(
                            step.getId(),
                            instance.getId(),
                            instance.getContractId(),
                            title,
                            step.getOrderIndex(),
                            allSteps.size(),
                            step.getCreatedAt()
                    );
                })
                .toList();
    }

    private String fetchContractTitle(Long contractId, HeaderPrincipal principal) {
        try {
            ContractDto contract = contractClient.getContract(
                    contractId, principal.userId(),
                    principal.organizationId(), principal.email(), principal.role());
            return contract.title();
        } catch (Exception e) {
            log.warn("Failed to fetch contract title for contractId={}: {}", contractId, e.getMessage());
            return "Sözleşme #" + contractId;
        }
    }

    private ApprovalStep firstPendingStep(List<ApprovalStep> steps) {
        if (steps == null) {
            return null;
        }
        return steps.stream()
                .filter(s -> s.getStatus() == StepStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private void validateStepAction(ApprovalStep step, WorkflowInstance instance,
                                    HeaderPrincipal principal) {
        if (instance.getCurrentState() != WorkflowState.PENDING_APPROVAL) {
            throw new InvalidApprovalActionException(
                    "Workflow onay bekleyen durumda değil: " + instance.getCurrentState());
        }

        if (step.getStatus() != StepStatus.PENDING) {
            throw new InvalidApprovalActionException(
                    "Bu adım zaten karara bağlanmış: " + step.getStatus());
        }

        ApprovalStep currentStep = approvalStepRepository
                .findFirstByWorkflowInstanceIdAndStatusOrderByOrderIndex(
                        instance.getId(), StepStatus.PENDING)
                .orElseThrow(() -> new WorkflowException("Bekleyen adım bulunamadı."));

        if (!currentStep.getId().equals(step.getId())) {
            throw new InvalidApprovalActionException(
                    "Sıradaki adım bu değil. Mevcut beklemede olan adım: " + currentStep.getOrderIndex());
        }

        if (!step.getApproverUserId().equals(principal.userId())) {
            throw new InvalidApprovalActionException(
                    "Bu adım için yetkili onaylayıcı siz değilsiniz.");
        }
    }
}
