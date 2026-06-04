package com.muhur.contract.repository;

import com.muhur.contract.domain.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, QuerydslPredicateExecutor<Contract> {

    Optional<Contract> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("""
            SELECT c FROM Contract c
            WHERE c.status = com.muhur.contract.domain.enums.ContractStatus.ACTIVE
              AND c.endDate IS NOT NULL
              AND c.endDate BETWEEN :from AND :to
            """)
    List<Contract> findExpiringActive(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to);
}
