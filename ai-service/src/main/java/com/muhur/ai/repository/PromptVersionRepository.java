package com.muhur.ai.repository;

import com.muhur.ai.domain.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    Optional<PromptVersion> findByPromptKeyAndIsActiveTrue(String promptKey);
}
