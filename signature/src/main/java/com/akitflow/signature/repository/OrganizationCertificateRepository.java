package com.akitflow.signature.repository;

import com.akitflow.signature.domain.OrganizationCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationCertificateRepository extends JpaRepository<OrganizationCertificate, Long> {

    Optional<OrganizationCertificate> findByOrganizationId(Long organizationId);
}
