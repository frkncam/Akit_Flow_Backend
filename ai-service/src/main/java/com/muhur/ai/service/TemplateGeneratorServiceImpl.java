package com.muhur.ai.service;

import com.muhur.ai.config.AppProperties;
import com.muhur.ai.domain.PromptVersion;
import com.muhur.ai.dto.request.GenerateTemplateRequest;
import com.muhur.ai.dto.response.GenerateTemplateResponse;
import com.muhur.ai.dto.response.GenerationMetadata;
import com.muhur.ai.exception.AiServiceUnavailableException;
import com.muhur.ai.exception.GenerationValidationException;
import com.muhur.ai.exception.RateLimitExceededException;
import com.muhur.ai.service.GroqClientService.GroqChatResult;
import com.muhur.ai.validator.TemplateDraftValidator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateGeneratorServiceImpl implements TemplateGeneratorService {

    private final GroqClientService groqClient;
    private final PromptTemplateService promptService;
    private final TemplateDraftValidator validator;
    private final GenerationLogService logService;
    private final AppProperties props;

    @Override
    @CircuitBreaker(name = "groq-api", fallbackMethod = "generationFallback")
    @Retry(name = "groq-api")
    public GenerateTemplateResponse generate(GenerateTemplateRequest request,
                                              Long userId, Long orgId) {
        log.info("Sablon uretimi basladi. orgId={}, userId={}, promptLength={}",
                orgId, userId, request.prompt().length());

        long start = System.currentTimeMillis();

        // 1. Aktif prompt'u yukle
        PromptVersion prompt = promptService.getActivePrompt();

        // 2. User prompt'unu formatla
        String userPrompt = promptService.formatUserPrompt(
                prompt,
                request.prompt(),
                request.language() != null ? request.language() : "tr"
        );

        // 3. Groq API cagrisi
        GroqChatResult result = groqClient.chat(prompt.getSystemPrompt(), userPrompt);

        // 4. Ilk validasyon: JSON parse
        GenerateTemplateResponse response;
        try {
            response = validator.validateAndParse(result.content());
        } catch (GenerationValidationException e) {
            // Ilk denemede validasyon hatasi → 1 kez retry
            log.warn("Ilk validasyon basarisiz, retry deneniyor. hatalar={}", e.getDetails());
            GroqChatResult retry = groqClient.chat(prompt.getSystemPrompt(),
                    userPrompt + "\n\nONCEKI yanitin su hatalari iceriyordu: "
                    + String.join("; ", e.getDetails()) + "\nLutfen bu hatalari duzelterek TEKRAR JSON uret.");
            // Iki cagrinin token toplamini birlestir
            result = new GroqChatResult(retry.content(), retry.model(),
                    result.promptTokens() + retry.promptTokens(),
                    result.completionTokens() + retry.completionTokens(),
                    result.durationMs() + retry.durationMs());
            response = validator.validateAndParse(result.content());
        }

        long duration = System.currentTimeMillis() - start;

        // 5. Basari logu
        logService.logSuccess(orgId, userId, prompt.getPromptKey(), prompt.getVersion(),
                result.model(), result.promptTokens(), result.completionTokens(),
                duration, request.prompt());

        log.info("Sablon uretimi basarili. orgId={}, durationMs={}, category={}, variableCount={}, totalTokens={}",
                orgId, duration, response.category(), response.variables().size(), result.totalTokens());

        // 6. Yanit metadata'sini gercek model/token/sure ile doldur (validator null/0/0 birakir)
        return new GenerateTemplateResponse(
                response.name(), response.description(), response.category(),
                response.bodyHtml(), response.variables(),
                new GenerationMetadata(result.model(), result.totalTokens(), duration));
    }

    /**
     * Circuit breaker aciksa veya tum retry'ler tukendiyse cagrilir.
     */
    private GenerateTemplateResponse generationFallback(GenerateTemplateRequest request,
                                                         Long userId, Long orgId,
                                                         Throwable t) {
        if (t instanceof GenerationValidationException ve) {
            throw ve;
        }
        if (t instanceof RateLimitExceededException re) {
            throw re;
        }
        log.error("Groq API kullanilamiyor. orgId={}, userId={}", orgId, userId, t);
        logService.logFailure(orgId, userId, props.prompt().defaultKey(), 1,
                props.groq().primaryModel(), 0, t.getMessage(), request.prompt());
        throw new AiServiceUnavailableException(
                "AI servisi su anda kullanilamiyor. Lutfen daha sonra tekrar deneyin."
        );
    }
}
