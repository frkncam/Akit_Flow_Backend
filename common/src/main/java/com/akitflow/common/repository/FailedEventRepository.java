package com.akitflow.common.repository;

import com.akitflow.common.domain.FailedEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    List<FailedEvent> findByTerminalFailureFalseOrderByCreatedAtAsc(Pageable pageable);

    @Modifying
    @Query("UPDATE FailedEvent f SET f.terminalFailure = true WHERE f.createdAt < :cutoff AND f.terminalFailure = false")
    int markTerminalFailuresOlderThan(@Param("cutoff") Instant cutoff);
}
