package com.akitflow.signature.repository;

import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.domain.enums.SignatureStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long>, QuerydslPredicateExecutor<Signature> {

    Optional<Signature> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Signature s WHERE s.id = :id")
    Optional<Signature> findByIdForUpdate(@Param("id") Long id);

    List<Signature> findAllByContractIdAndStatus(Long contractId, SignatureStatus status);

    @Query("SELECT s FROM Signature s WHERE s.contractId = :contractId AND s.organizationId = :organizationId AND s.status != 'CANCELLED'")
    List<Signature> findAllByContractIdAndOrganizationId(@Param("contractId") Long contractId, @Param("organizationId") Long organizationId);

    List<Signature> findAllByContractIdAndBatchId(Long contractId, UUID batchId);

    List<Signature> findAllByContractIdAndBatchIdAndStatus(Long contractId, UUID batchId, SignatureStatus status);
}
