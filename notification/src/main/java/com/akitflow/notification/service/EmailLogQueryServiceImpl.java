package com.akitflow.notification.service;

import com.akitflow.notification.domain.EmailLog;
import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.dto.response.EmailLogResponse;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.notification.mapper.EmailLogMapper;
import com.akitflow.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailLogQueryServiceImpl implements EmailLogQueryService {

    private final EmailLogRepository repository;
    private final EmailLogMapper mapper;

    @Override
    public Page<EmailLogResponse> findAll(EmailStatus status, Pageable pageable) {
        Page<EmailLog> result = (status != null)
                ? repository.findAllByStatus(status, pageable)
                : repository.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Override
    public EmailLogResponse findById(Long id) {
        EmailLog entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EmailLog not found with id: " + id));
        return mapper.toResponse(entity);
    }
}
