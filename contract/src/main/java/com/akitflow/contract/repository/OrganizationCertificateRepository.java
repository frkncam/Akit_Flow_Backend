package com.akitflow.contract.repository;

import com.akitflow.contract.domain.OrganizationCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationCertificateRepository extends JpaRepository<OrganizationCertificate, Long> {

    Optional<OrganizationCertificate> findByOrganizationId(Long organizationId);
}
