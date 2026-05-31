package com.akitflow.template.repository;

import com.akitflow.template.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long>, QuerydslPredicateExecutor<Template> {

    Optional<Template> findByIdAndOrganizationId(Long id, Long organizationId);
}
