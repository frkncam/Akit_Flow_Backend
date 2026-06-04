package com.muhur.ai.repository;

import com.muhur.ai.domain.GenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationLogRepository extends JpaRepository<GenerationLog, Long> {
}
