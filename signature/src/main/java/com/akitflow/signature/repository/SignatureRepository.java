package com.akitflow.signature.repository;

import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.domain.enums.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {

    Optional<Signature> findByToken(String token);

    List<Signature> findAllByContractIdAndStatus(Long contractId, SignatureStatus status);

    List<Signature> findAllByContractId(Long contractId);

    List<Signature> findAllByContractIdAndBatchId(Long contractId, UUID batchId);

    List<Signature> findAllByContractIdAndBatchIdAndStatus(Long contractId, UUID batchId, SignatureStatus status);
}
