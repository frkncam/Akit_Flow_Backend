package com.muhur.notification.repository;

import com.muhur.notification.domain.EmailLog;
import com.muhur.notification.domain.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Page<EmailLog> findAllByStatus(EmailStatus status, Pageable pageable);
}
