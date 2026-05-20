package com.akitflow.notification.repository;

import com.akitflow.notification.domain.EmailLog;
import com.akitflow.notification.domain.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Page<EmailLog> findAllByStatus(EmailStatus status, Pageable pageable);
}
