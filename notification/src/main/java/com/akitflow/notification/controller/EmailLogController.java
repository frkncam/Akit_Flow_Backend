package com.akitflow.notification.controller;

import com.akitflow.notification.domain.enums.EmailStatus;
import com.akitflow.notification.dto.response.EmailLogResponse;
import com.akitflow.notification.service.EmailLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/emails")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogQueryService emailLogQueryService;

    @GetMapping
    public List<EmailLogResponse> list(@RequestParam(required = false) EmailStatus status) {
        return emailLogQueryService.findAll(status);
    }

    @GetMapping("/{id}")
    public EmailLogResponse getById(@PathVariable Long id) {
        return emailLogQueryService.findById(id);
    }
}
