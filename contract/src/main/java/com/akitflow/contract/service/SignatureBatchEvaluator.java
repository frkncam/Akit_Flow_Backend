package com.akitflow.contract.service;

import com.akitflow.contract.domain.ContractSignature;
import com.akitflow.contract.domain.enums.SignatureStatus;
import com.akitflow.contract.repository.ContractSignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureBatchEvaluator {

    private final ContractSignatureRepository signatureRepository;

    @Transactional(readOnly = true)
    public BatchStatus evaluate(Long contractId, UUID batchId) {
        List<ContractSignature> batch =
                signatureRepository.findAllByContract_IdAndBatchId(contractId, batchId);

        boolean anyPending  = batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.PENDING);
        boolean anyRejected = batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.REJECTED);
        boolean allSigned   = !anyPending
                && !anyRejected
                && batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.SIGNED);

        return new BatchStatus(allSigned, anyPending, anyRejected);
    }

    public record BatchStatus(boolean allSigned, boolean anyPending, boolean anyRejected) {}
}
