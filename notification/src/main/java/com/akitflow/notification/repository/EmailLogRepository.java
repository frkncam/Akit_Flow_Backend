package com.akitflow.notification.repository;

import com.akitflow.notification.domain.EmailLog;
import com.akitflow.notification.domain.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findAllByStatus(EmailStatus status);
}
