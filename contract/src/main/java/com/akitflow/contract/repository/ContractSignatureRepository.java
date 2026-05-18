package com.akitflow.contract.repository;

import com.akitflow.contract.domain.ContractSignature;
import com.akitflow.contract.domain.enums.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    Optional<ContractSignature> findByToken(String token);

    List<ContractSignature> findAllByContract_Id(Long contractId);

    List<ContractSignature> findAllByContract_IdAndStatus(Long contractId, SignatureStatus status);

    /**
     * Batch-scoped: only signatures from a single send-for-signature round.
     * Used by accept/reject to evaluate "all signed?" without contamination
     * from previous rounds' rejected/cancelled rows.
     */
    List<ContractSignature> findAllByContract_IdAndBatchId(Long contractId, UUID batchId);

    List<ContractSignature> findAllByContract_IdAndBatchIdAndStatus(
            Long contractId, UUID batchId, SignatureStatus status);
}
