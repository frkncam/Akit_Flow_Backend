package com.akitflow.notification.service;

import com.akitflow.notification.domain.EmailLog;
import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.dto.response.EmailLogResponse;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.notification.mapper.EmailLogMapper;
import com.akitflow.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailLogQueryServiceImpl implements EmailLogQueryService {

    private final EmailLogRepository repository;
    private final EmailLogMapper mapper;

    @Override
    public List<EmailLogResponse> findAll(EmailStatus status) {
        List<EmailLog> result = (status != null)
                ? repository.findAllByStatus(status)
                : repository.findAll();
        return mapper.toResponseList(result);
    }

    @Override
    public EmailLogResponse findById(Long id) {
        EmailLog entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EmailLog not found with id: " + id));
        return mapper.toResponse(entity);
    }
}
