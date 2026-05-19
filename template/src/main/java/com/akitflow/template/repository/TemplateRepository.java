package com.akitflow.template.repository;

import com.akitflow.template.domain.Template;
import com.akitflow.template.domain.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findAllByOrganizationId(Long organizationId);

    List<Template> findAllByOrganizationIdAndCategory(Long organizationId, TemplateCategory category);

    Optional<Template> findByIdAndOrganizationId(Long id, Long organizationId);
}
