package com.muhur.ai.service;

import com.muhur.ai.config.AppProperties;
import com.muhur.ai.domain.PromptVersion;
import com.muhur.ai.dto.request.GenerateTemplateRequest;
import com.muhur.ai.dto.response.GenerateTemplateResponse;
import com.muhur.ai.dto.response.GenerationMetadata;
import com.muhur.ai.exception.GenerationValidationException;
import com.muhur.ai.service.GroqClientService.GroqChatResult;
import com.muhur.ai.validator.TemplateDraftValidator;
import com.muhur.common.client.dto.TemplateVariableDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateGeneratorServiceImplTest {

    @Mock
    private GroqClientService groqClient;

    @Mock
    private PromptTemplateService promptService;

    @Mock
    private TemplateDraftValidator validator;

    @Mock
    private GenerationLogService logService;

    @Mock
    private AppProperties props;

    @InjectMocks
    private TemplateGeneratorServiceImpl service;

    private PromptVersion promptVersion;
    private GenerateTemplateRequest request;
    private GenerateTemplateResponse mockResponse;

    @BeforeEach
    void setUp() {
        service = new TemplateGeneratorServiceImpl(
                groqClient, promptService, validator, logService, props);

        promptVersion = PromptVersion.builder()
                .promptKey("template-generation-v1")
                .version(1)
                .systemPrompt("Test system prompt")
                .userTemplate("User request: {prompt}")
                .build();

        request = new GenerateTemplateRequest("Test sozlesmesi", "tr");

        mockResponse = new GenerateTemplateResponse(
                "Test Sozlesme", "Test aciklamasi", "RENTAL",
                "<p>{{ var1 }}</p><p>{{ var2 }}</p><p>{{ var3 }}</p>",
                List.of(
                        new TemplateVariableDto("var1", "V1", "TEXT", "CUSTOM", null, null, true, 1),
                        new TemplateVariableDto("var2", "V2", "TEXT", "CUSTOM", null, null, true, 2),
                        new TemplateVariableDto("var3", "V3", "TEXT", "CUSTOM", null, null, true, 3)
                ),
                new GenerationMetadata("llama-3.3-70b-versatile", 500, 3000)
        );
    }

    @Test
    @DisplayName("Basarili uretim — tum adimlar dogru sirada cagrilmali")
    void successfulGenerationShouldCallAllSteps() {
        when(promptService.getActivePrompt()).thenReturn(promptVersion);
        when(promptService.formatUserPrompt(any(), anyString(), anyString()))
                .thenReturn("User request: Test sozlesmesi");
        when(groqClient.chat(anyString(), anyString()))
                .thenReturn(new GroqChatResult("{}", "llama-3.3-70b-versatile", 100, 200, 3000));
        when(validator.validateAndParse(anyString())).thenReturn(mockResponse);

        GenerateTemplateResponse response = service.generate(request, 1L, 1L);

        assertThat(response.name()).isEqualTo("Test Sozlesme");
        assertThat(response.category()).isEqualTo("RENTAL");

        verify(promptService).getActivePrompt();
        verify(promptService).formatUserPrompt(any(), anyString(), anyString());
        verify(groqClient).chat(anyString(), anyString());
        verify(validator).validateAndParse(anyString());
        verify(logService).logSuccess(anyLong(), anyLong(), anyString(), anyInt(),
                anyString(), anyInt(), anyInt(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Validasyon hatasinda retry denenmeli")
    void validationErrorShouldTriggerRetry() {
        when(promptService.getActivePrompt()).thenReturn(promptVersion);
        when(promptService.formatUserPrompt(any(), anyString(), anyString()))
                .thenReturn("User request: Test sozlesmesi");
        when(groqClient.chat(anyString(), anyString()))
                .thenReturn(new GroqChatResult("{}", "llama-3.3-70b-versatile", 100, 200, 3000));
        when(validator.validateAndParse(anyString()))
                .thenThrow(new GenerationValidationException(List.of("Hata 1")))
                .thenReturn(mockResponse);

        GenerateTemplateResponse response = service.generate(request, 1L, 1L);

        assertThat(response.name()).isEqualTo("Test Sozlesme");
        verify(groqClient, times(2)).chat(anyString(), anyString());
        verify(validator, times(2)).validateAndParse(anyString());
    }
}
