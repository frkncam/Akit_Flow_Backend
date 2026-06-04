package com.muhur.signature.service;

import com.muhur.signature.domain.Signature;
import com.muhur.signature.domain.enums.SignatureStatus;
import com.muhur.signature.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureBatchEvaluator {

    private final SignatureRepository signatureRepository;

    @Transactional(readOnly = true)
    public BatchStatus evaluate(Long contractId, UUID batchId) {
        List<Signature> batch =
                signatureRepository.findAllByContractIdAndBatchId(contractId, batchId);

        boolean anyPending  = batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.PENDING);
        boolean anyRejected = batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.REJECTED);
        boolean allSigned   = !anyPending
                && !anyRejected
                && batch.stream().anyMatch(s -> s.getStatus() == SignatureStatus.SIGNED);

        return new BatchStatus(allSigned, anyPending, anyRejected);
    }

    public record BatchStatus(boolean allSigned, boolean anyPending, boolean anyRejected) {}
}
