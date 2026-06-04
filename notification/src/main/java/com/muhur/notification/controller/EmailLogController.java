package com.muhur.notification.controller;

import com.muhur.notification.domain.enums.EmailStatus;
import com.muhur.notification.dto.response.EmailLogResponse;
import com.muhur.notification.service.EmailLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/emails")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogQueryService emailLogQueryService;

    @GetMapping
    public Page<EmailLogResponse> list(@RequestParam(required = false) EmailStatus status,
                                       Pageable pageable) {
        return emailLogQueryService.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public EmailLogResponse getById(@PathVariable Long id) {
        return emailLogQueryService.findById(id);
    }
}
