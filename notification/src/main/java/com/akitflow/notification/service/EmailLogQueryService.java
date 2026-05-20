package com.akitflow.notification.service;

import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.dto.response.EmailLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmailLogQueryService {

    Page<EmailLogResponse> findAll(EmailStatus status, Pageable pageable);

    EmailLogResponse findById(Long id);
}
