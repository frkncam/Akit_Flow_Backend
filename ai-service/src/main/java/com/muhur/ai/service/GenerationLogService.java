package com.muhur.ai.service;

import com.muhur.ai.domain.GenerationLog;
import com.muhur.ai.repository.GenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GenerationLogService {

    private final GenerationLogRepository repository;

    public void logSuccess(Long orgId, Long userId, String promptKey, int version,
                           String model, int promptTokens, int completionTokens,
                           long durationMs, String userPrompt) {
        GenerationLog logEntry = GenerationLog.builder()
                .organizationId(orgId)
                .userId(userId)
                .promptKey(promptKey)
                .promptVersion(version)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .durationMs(durationMs)
                .success(true)
                .userPromptHash(sha256(userPrompt))
                .build();
        repository.save(logEntry);
    }

    public void logFailure(Long orgId, Long userId, String promptKey, int version,
                           String model, int promptTokens, String errorMessage, String userPrompt) {
        GenerationLog logEntry = GenerationLog.builder()
                .organizationId(orgId)
                .userId(userId)
                .promptKey(promptKey)
                .promptVersion(version)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(0)
                .totalTokens(promptTokens)
                .durationMs(0L)
                .success(false)
                .errorMessage(errorMessage)
                .userPromptHash(sha256(userPrompt))
                .build();
        repository.save(logEntry);
    }

    private String sha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
