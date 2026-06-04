package com.muhur.ai.service;

import com.muhur.ai.config.AppProperties;
import com.muhur.ai.domain.PromptVersion;
import com.muhur.ai.repository.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PromptTemplateService {

    private final PromptVersionRepository repository;
    private final AppProperties props;

    /**
     * Aktif prompt'u DB'den yukler.
     * Varsayilan olarak 'template-generation-v1' key'ini kullanir.
     */
    public PromptVersion getActivePrompt() {
        return getActivePrompt(props.prompt().defaultKey());
    }

    /**
     * Belirli bir key icin aktif prompt'u yukler.
     */
    public PromptVersion getActivePrompt(String promptKey) {
        return repository.findByPromptKeyAndIsActiveTrue(promptKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Aktif prompt bulunamadi: key=" + promptKey
                                + ". Lutfen ai_schema.prompt_versions tablosuna seed data ekleyin."));
    }

    /**
     * User prompt template'ini formatlar.
     * {prompt} → kullanicinin talebi
     * {language} → dil kodu
     */
    public String formatUserPrompt(PromptVersion prompt, String userRequest, String language) {
        return prompt.getUserTemplate()
                .replace("{prompt}", userRequest)
                .replace("{language}", language != null ? language : "tr");
    }
}
