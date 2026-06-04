package com.muhur.contract.event.listener;

import com.muhur.contract.domain.Contract;
import com.muhur.contract.domain.ProcessedEvent;
import com.muhur.contract.domain.enums.ContractStatus;
import com.muhur.contract.repository.ContractRepository;
import com.muhur.contract.repository.ProcessedEventRepository;
import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.SignatureBatchCompletedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("SignatureBatchCompletedListener — PENDING_SIGNATURE → ACTIVE transition")
class SignatureBatchCompletedListenerTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private SignatureBatchCompletedListener listener;

    private static final Long CONTRACT_ID = 1L;
    private static final UUID EVENT_ID = UUID.randomUUID();

    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = Contract.builder()
                .id(CONTRACT_ID)
                .status(ContractStatus.PENDING_SIGNATURE)
                .organizationId(1L)
                .title("Test Contract")
                .createdBy(1L)
                .build();
    }

    @Test
    @DisplayName("PENDING_SIGNATURE → ACTIVE on signature batch completed")
    void pendingSignatureTransitionsToActive() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event());

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getSignedAt()).isNotNull();
        verify(contractRepository).save(contract);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("invalid transition DRAFT → ACTIVE is skipped")
    void invalidTransitionIsSkipped() {
        contract.setStatus(ContractStatus.DRAFT);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        listener.onMessage(event());

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("duplicate event → skip")
    void duplicateEventIsSkipped() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

        listener.onMessage(event());

        verify(contractRepository, never()).findById(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("contract not found → warn")
    void contractNotFoundLogsWarning() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.empty());

        listener.onMessage(event());

        verify(contractRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("full flow: APPROVED → PENDING_SIGNATURE (via sendForSignature) → ACTIVE (via listener)")
    void fullApprovalToActiveFlow() {
        contract.setStatus(ContractStatus.APPROVED);
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(contractRepository.findById(CONTRACT_ID)).thenReturn(Optional.of(contract));

        contract.transitionTo(ContractStatus.PENDING_SIGNATURE);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING_SIGNATURE);

        listener.onMessage(event());

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getSignedAt()).isNotNull();
    }

    private DomainEvent<SignatureBatchCompletedPayload> event() {
        return new DomainEvent<>(
                EVENT_ID,
                "signature.batch.completed",
                Instant.now(),
                1L,
                1L,
                new SignatureBatchCompletedPayload(CONTRACT_ID, "Test Contract", "creator@test.com", java.util.List.of("signer@test.com"))
        );
    }
}
