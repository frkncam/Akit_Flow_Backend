package com.muhur.contract.event.listener;

import com.muhur.contract.domain.Contract;
import com.muhur.contract.domain.ProcessedEvent;
import com.muhur.contract.domain.enums.ContractStatus;
import com.muhur.contract.repository.ContractRepository;
import com.muhur.contract.repository.ProcessedEventRepository;
import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.WorkflowTransitionedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowTransitionedListener — workflow state → contract status mapping")
class WorkflowTransitionedListenerTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private WorkflowTransitionedListener listener;

    private static final Long CONTRACT_ID = 1L;
    private static final UUID EVENT_ID = UUID.randomUUID();

    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = Contract.builder()
                .id(CONTRACT_ID)
                .status(ContractStatus.IN_REVIEW)
                .organizationId(1L)
                .title("Test Contract")
                .createdBy(1L)
                .build();
    }

    @Test
    @DisplayName("PENDING_APPROVAL → IN_REVIEW")
    void pendingApprovalMapsToInReview() {
        contract.setStatus(ContractStatus.DRAFT);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event("PENDING_APPROVAL", "DRAFT"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.IN_REVIEW);
        verify(contractRepository).save(contract);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("APPROVED → APPROVED")
    void approvedMapsToApproved() {
        contract.setStatus(ContractStatus.IN_REVIEW);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event("APPROVED", "PENDING_APPROVAL"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.APPROVED);
    }

    @Test
    @DisplayName("REJECTED → REJECTED")
    void rejectedMapsToRejected() {
        contract.setStatus(ContractStatus.IN_REVIEW);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event("REJECTED", "PENDING_APPROVAL"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.REJECTED);
    }

    @Test
    @DisplayName("CANCELLED → DRAFT")
    void cancelledMapsToDraft() {
        contract.setStatus(ContractStatus.IN_REVIEW);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event("CANCELLED", "PENDING_APPROVAL"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
    }

    @Test
    @DisplayName("unknown workflow state → skip, still persist processed event")
    void unknownWorkflowStateSkipsTransition() {
        contract.setStatus(ContractStatus.DRAFT);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);

        listener.onMessage(event("UNKNOWN_STATE", "DRAFT"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        verify(contractRepository, never()).save(any(Contract.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("invalid transition → skip with warning")
    void invalidTransitionIsSkipped() {
        contract.setStatus(ContractStatus.ACTIVE);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event("APPROVED", "PENDING_APPROVAL"));

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("duplicate event → skip entirely")
    void duplicateEventIsSkipped() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

        listener.onMessage(event("APPROVED", "PENDING_APPROVAL"));

        verify(contractRepository, never()).findById(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("contract not found → warn")
    void contractNotFoundLogsWarning() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.empty());

        listener.onMessage(event("APPROVED", "PENDING_APPROVAL"));

        verify(contractRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("DRAFT workflow state → no contract status change")
    void draftWorkflowStateDoesNotChangeContractStatus() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);

        listener.onMessage(event("DRAFT", "CANCELLED"));

        verify(contractRepository, never()).findById(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    private DomainEvent<WorkflowTransitionedPayload> event(String toState, String fromState) {
        return new DomainEvent<>(
                EVENT_ID,
                "workflow.transitioned",
                Instant.now(),
                1L,
                1L,
                new WorkflowTransitionedPayload(CONTRACT_ID, fromState, toState, "APPROVE")
        );
    }
}
