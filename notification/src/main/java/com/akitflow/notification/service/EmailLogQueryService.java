package com.akitflow.notification.service;

import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.dto.response.EmailLogResponse;

import java.util.List;

public interface EmailLogQueryService {

    List<EmailLogResponse> findAll(EmailStatus status);

    EmailLogResponse findById(Long id);
}
