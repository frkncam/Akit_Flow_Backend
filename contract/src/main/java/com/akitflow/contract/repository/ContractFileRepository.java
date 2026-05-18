package com.akitflow.contract.repository;

import com.akitflow.contract.domain.ContractFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractFileRepository extends JpaRepository<ContractFile, Long> {

    List<ContractFile> findAllByContract_IdOrderByVersionDesc(Long contractId);

    Optional<ContractFile> findByIdAndContract_OrganizationId(Long id, Long organizationId);

    List<ContractFile> findAllByContract_Id(Long contractId);

    @Query("SELECT COALESCE(MAX(f.version), 0) FROM ContractFile f WHERE f.contract.id = :contractId")
    Integer findMaxVersionByContractId(@Param("contractId") Long contractId);
}
