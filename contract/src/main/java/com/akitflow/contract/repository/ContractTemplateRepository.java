package com.akitflow.contract.repository;

import com.akitflow.contract.domain.ContractTemplate;
import com.akitflow.contract.domain.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    List<ContractTemplate> findAllByOrganizationId(Long organizationId);

    List<ContractTemplate> findAllByOrganizationIdAndCategory(Long organizationId, TemplateCategory category);

    Optional<ContractTemplate> findByIdAndOrganizationId(Long id, Long organizationId);
}
