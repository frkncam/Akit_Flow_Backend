package com.muhur.ai.service;

import com.muhur.ai.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqClientService {

    private final OpenAiChatModel chatModel;
    private final AppProperties props;

    /**
     * Groq cagrisinin sonucu: ham icerik + token kullanimi + sure.
     */
    public record GroqChatResult(
            String content,
            String model,
            int promptTokens,
            int completionTokens,
            long durationMs
    ) {
        public int totalTokens() {
            return promptTokens + completionTokens;
        }
    }

    /**
     * Groq API'ye chat istegi gonderir.
     *
     * @param systemPrompt sistem prompt'u (DB'den yuklenen)
     * @param userPrompt   kullanicinin talebinden formatlanan user prompt'u
     * @return ham icerik (JSON string olmali) + token/latency metadata'si
     */
    public GroqChatResult chat(String systemPrompt, String userPrompt) {
        log.debug("Groq API cagrisi basliyor. model={}, systemPromptLength={}, userPromptLength={}",
                props.groq().primaryModel(), systemPrompt.length(), userPrompt.length());

        long start = System.currentTimeMillis();

        ChatResponse response = chatModel.call(
                new Prompt(
                        List.of(
                                new SystemMessage(systemPrompt),
                                new UserMessage(userPrompt)
                        ),
                        OpenAiChatOptions.builder()
                                .model(props.groq().primaryModel())
                                .temperature(0.3)
                                .maxTokens(8192)
                                .responseFormat(
                                        ResponseFormat.builder()
                                                .type(ResponseFormat.Type.JSON_OBJECT)
                                                .build()
                                )
                                .build()
                )
        );

        long duration = System.currentTimeMillis() - start;
        String content = response.getResult().getOutput().getText();

        Usage usage = response.getMetadata().getUsage();
        int promptTokens = usage != null ? usage.getPromptTokens() : 0;
        int completionTokens = usage != null ? usage.getCompletionTokens() : 0;

        log.info("Groq API yaniti alindi. model={}, durationMs={}, responseLength={}, "
                 + "promptTokens={}, completionTokens={}",
                props.groq().primaryModel(), duration, content.length(),
                promptTokens, completionTokens);

        return new GroqChatResult(content, props.groq().primaryModel(),
                promptTokens, completionTokens, duration);
    }
}
