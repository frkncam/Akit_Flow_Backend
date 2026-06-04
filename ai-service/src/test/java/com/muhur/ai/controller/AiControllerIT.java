package com.muhur.ai.controller;

import com.muhur.ai.domain.PromptVersion;
import com.muhur.ai.repository.PromptVersionRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

/**
 * Entegrasyon testi — Groq API WireMock ile mock'lanir.
 * Test ortami (H2, eureka kapali, liquibase kapali vb.) src/test/resources/application.yaml'da.
 * Iki test ayni Spring context'ini (dolayisiyla ayni circuit breaker'i) paylasir; bu yuzden
 * her testten once @BeforeEach ile 'groq-api' breaker'i RESET edilir — aksi halde circuit
 * acan test, basari testini CallNotPermitted ile zehirler.
 */
// NOT: RabbitAutoConfiguration DISLANMAZ — common modulundeki FailedEventRetryScheduler /
// TransactionAwareEventPublisher RabbitTemplate'e ihtiyac duyar. RabbitAutoConfiguration
// RabbitTemplate'i lazy (broker'a baglanmadan) olusturur; test hicbir sey publish etmedigi
// icin gercek baglanti denenmez. Test ortam ayarlari src/test/resources/application.yaml'da.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "eureka.client.enabled=false",
            "spring.liquibase.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
class AiControllerIT {

    private static WireMockServer wireMock;

    @DynamicPropertySource
    static void overrideGroqUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url",
                () -> "http://localhost:" + wireMock.port() + "/openai");
    }

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().port(0));
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private PromptVersionRepository promptVersionRepository;

    @BeforeEach
    void setUp() {
        // Testler context'i (ve circuit breaker'i) paylasir; her testi temiz CLOSED durumda baslat.
        circuitBreakerRegistry.circuitBreaker("groq-api").reset();
        wireMock.resetAll();
        // Liquibase test'te kapali oldugu icin seed (002-seed-prompt.sql) calismaz;
        // aktif prompt'u programatik ekle (yoksa getActivePrompt() patlar → 503).
        if (promptVersionRepository.findByPromptKeyAndIsActiveTrue("template-generation-v1").isEmpty()) {
            promptVersionRepository.save(PromptVersion.builder()
                    .promptKey("template-generation-v1")
                    .version(1)
                    .systemPrompt("Test sistem prompt'u")
                    .userTemplate("Kullanici talebi: {prompt}")
                    .isActive(true)
                    .build());
        }
    }

    @Test
    @DisplayName("Gecerli prompt → 200 ve sablon donmeli")
    void validPromptShouldReturn200() {
        wireMock.stubFor(post(urlEqualTo("/openai/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                            "id": "chatcmpl-test",
                            "object": "chat.completion",
                            "created": 1700000000,
                            "model": "llama-3.3-70b-versatile",
                            "choices": [{
                                "index": 0,
                                "finish_reason": "stop",
                                "message": {
                                    "role": "assistant",
                                    "content": "{\\"name\\":\\"Test Sozlesme\\",\\"category\\":\\"RENTAL\\",\\"description\\":\\"Test\\",\\"bodyHtml\\":\\"<p>{{ v1 }}</p><p>{{ v2 }}</p><p>{{ v3 }}</p>\\",\\"variables\\":[{\\"key\\":\\"v1\\",\\"label\\":\\"V1\\",\\"type\\":\\"TEXT\\",\\"required\\":true,\\"position\\":1},{\\"key\\":\\"v2\\",\\"label\\":\\"V2\\",\\"type\\":\\"TEXT\\",\\"required\\":true,\\"position\\":2},{\\"key\\":\\"v3\\",\\"label\\":\\"V3\\",\\"type\\":\\"TEXT\\",\\"required\\":true,\\"position\\":3}]}"
                                }
                            }],
                            "usage": {"prompt_tokens": 100, "completion_tokens": 200, "total_tokens": 300}
                        }
                        """)));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        headers.set("X-Org-Id", "1");
        headers.set("X-User-Email", "test@test.com");
        headers.set("X-User-Role", "OWNER");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(
                "{\"prompt\":\"Test sozlesmesi\",\"language\":\"tr\"}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/ai/generate-template", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Test Sozlesme");
        // metadata gercek model + token (usage 100+200=300) ile dolmali
        assertThat(response.getBody()).contains("llama-3.3-70b-versatile");
        assertThat(response.getBody()).contains("\"tokensUsed\":300");
    }

    @Test
    @DisplayName("Groq API hata verirse circuit breaker acilmali")
    void groqApiErrorShouldTriggerCircuitBreaker() {
        wireMock.stubFor(post(urlEqualTo("/openai/v1/chat/completions"))
                .willReturn(aResponse().withStatus(500)));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        headers.set("X-Org-Id", "1");
        headers.set("X-User-Email", "test@test.com");
        headers.set("X-User-Role", "OWNER");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(
                "{\"prompt\":\"Test\",\"language\":\"tr\"}", headers);

        // Ilk istek — retry denenir
        restTemplate.postForEntity("/api/v1/ai/generate-template", request, String.class);

        // Circuit breaker acilana kadar birkac istek daha
        for (int i = 0; i < 15; i++) {
            restTemplate.postForEntity("/api/v1/ai/generate-template", request, String.class);
        }

        ResponseEntity<String> responseFinal = restTemplate.postForEntity(
                "/api/v1/ai/generate-template", request, String.class);

        assertThat(responseFinal.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
