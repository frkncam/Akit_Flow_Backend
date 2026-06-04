package com.muhur.ai.validator;

import com.muhur.ai.config.AppProperties;
import com.muhur.ai.dto.response.GenerateTemplateResponse;
import com.muhur.ai.dto.response.GenerationMetadata;
import com.muhur.ai.exception.GenerationValidationException;
import com.muhur.common.client.dto.TemplateVariableDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateDraftValidator {

    private final ObjectMapper objectMapper;
    private final AppProperties props;

    // TemplateRenderer ile AYNI regex — {{ variable_name }}
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\{\\{\\s*([a-z][a-z0-9_]*)\\s*\\}\\}"
    );

    private static final Set<String> VALID_TYPES = Set.of("TEXT", "MULTILINE", "NUMBER", "DATE", "CURRENCY");
    private static final Set<String> VALID_CATEGORIES = Set.of("NDA", "EMPLOYMENT", "SERVICE", "RENTAL", "OTHER");
    private static final Set<String> VALID_SOURCES = Set.of("CUSTOM", "SYSTEM");

    // XSS taramasi icin regex
    private static final Pattern EVENT_HANDLER = Pattern.compile(
            "\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JAVASCRIPT_URL = Pattern.compile(
            "javascript\\s*:", Pattern.CASE_INSENSITIVE
    );

    /**
     * LLM ham JSON ciktisini valide eder ve GenerateTemplateResponse'a donusturur.
     *
     * @throws GenerationValidationException validasyon hatalarinin listesiyle birlikte
     */
    public GenerateTemplateResponse validateAndParse(String rawJson) {
        List<String> errors = new ArrayList<>();

        // Adim 1: JSON parse
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new GenerationValidationException(
                    List.of("LLM yaniti gecerli JSON degil: " + e.getMessage())
            );
        }

        // Adim 2: Zorunlu alanlar
        String name = extractString(root, "name", errors);
        String description = root.has("description") && !root.get("description").isNull()
                ? root.get("description").asText() : null;
        String category = extractString(root, "category", errors);
        String bodyHtml = extractString(root, "bodyHtml", errors);
        JsonNode variablesNode = root.get("variables");

        // Adim 3: Name kontrolu
        if (name != null && (name.isBlank() || name.length() > 255)) {
            errors.add("name bos veya 255 karakterden uzun");
        }

        // Adim 4: Kategori validasyonu
        if (category != null && !VALID_CATEGORIES.contains(category.toUpperCase())) {
            log.warn("Gecersiz kategori '{}', OTHER olarak duzeltildi", category);
            category = "OTHER";
        }

        // Adim 5: bodyHtml validasyonu
        List<TemplateVariableDto> variables = List.of();
        if (bodyHtml != null) {
            // Boyut kontrolu — char degil gercek UTF-8 byte
            int bodyBytes = bodyHtml.getBytes(StandardCharsets.UTF_8).length;
            if (bodyBytes > props.validation().maxBodySizeBytes()) {
                errors.add("bodyHtml cok buyuk: " + bodyBytes + " byte (max: "
                        + props.validation().maxBodySizeBytes() + ")");
            }

            // HTML well-formed kontrolu (Jsoup)
            try {
                Document doc = Jsoup.parse(bodyHtml);
                if (!doc.parser().getErrors().isEmpty()) {
                    String parserErrors = doc.parser().getErrors().stream()
                            .limit(3)
                            .map(e -> e.getErrorMessage())
                            .collect(Collectors.joining("; "));
                    errors.add("HTML parse hatalari: " + parserErrors);
                }

                // <script> etiketi kontrolu
                if (!doc.select("script").isEmpty()) {
                    errors.add("bodyHtml <script> etiketi iceriyor — guvenlik nedeniyle reddedildi");
                }
            } catch (Exception e) {
                errors.add("HTML parse edilemedi: " + e.getMessage());
            }

            // Event handler kontrolu
            if (EVENT_HANDLER.matcher(bodyHtml).find()) {
                errors.add("bodyHtml event handler (onclick, onerror vb.) iceriyor");
            }

            // javascript: URL kontrolu
            if (JAVASCRIPT_URL.matcher(bodyHtml).find()) {
                errors.add("bodyHtml 'javascript:' URL'si iceriyor");
            }
        }

        // Adim 6: Variables validasyonu
        if (variablesNode != null && variablesNode.isArray()) {
            variables = validateVariables(variablesNode, bodyHtml, errors);
        } else {
            errors.add("variables eksik veya array degil");
        }

        // Adim 7: Tum kontroller sonrasi
        if (!errors.isEmpty()) {
            throw new GenerationValidationException(errors);
        }

        // Basarili
        return new GenerateTemplateResponse(
                name, description, category != null ? category.toUpperCase() : "OTHER",
                bodyHtml, variables,
                new GenerationMetadata(null, 0, 0)
        );
    }

    private List<TemplateVariableDto> validateVariables(JsonNode variablesNode, String bodyHtml,
                                                         List<String> errors) {
        List<TemplateVariableDto> variables = new ArrayList<>();
        Set<String> keys = new HashSet<>();

        for (int i = 0; i < variablesNode.size(); i++) {
            JsonNode v = variablesNode.get(i);
            String key = v.has("key") ? v.get("key").asText() : null;
            String label = v.has("label") ? v.get("label").asText() : null;
            String type = v.has("type") ? v.get("type").asText().toUpperCase() : null;
            String source = v.has("source") ? v.get("source").asText().toUpperCase() : "CUSTOM";
            boolean required = v.has("required") && v.get("required").asBoolean();
            int position = v.has("position") ? v.get("position").asInt() : i + 1;
            String defaultValue = (v.has("defaultValue") && !v.get("defaultValue").isNull())
                    ? v.get("defaultValue").asText() : null;
            if (defaultValue != null && defaultValue.isBlank()) {
                defaultValue = null;
            }

            // Key validasyonu
            if (key == null || key.isBlank()) {
                errors.add("variables[" + i + "].key eksik");
                continue;
            }
            if (!key.matches("[a-z][a-z0-9_]*")) {
                errors.add("variables[" + i + "].key gecersiz format: '" + key
                        + "' (sadece snake_case Ingilizce)");
            }

            // Duplicate key kontrolu — ilk gecen alinir, sonrakiler atlanir
            if (!keys.add(key)) {
                log.warn("variables[{}].key tekrar ediyor: '{}' — atlandi", i, key);
                continue;
            }

            // Type validasyonu
            if (type == null || !VALID_TYPES.contains(type)) {
                log.warn("variables[{}].type gecersiz '{}', TEXT olarak duzeltildi", i, type);
                type = "TEXT";
            }

            // Label validasyonu
            if (label == null || label.isBlank()) {
                label = key;
            }

            // Source validasyonu
            if (!VALID_SOURCES.contains(source)) {
                source = "CUSTOM";
            }

            // Position duzeltme
            if (position < 1) position = i + 1;

            variables.add(new TemplateVariableDto(
                    key, label, type, source, null, defaultValue, required, position
            ));
        }

        // Degisken sayisi kontrolu
        int count = variables.size();
        if (count < props.validation().minVariables()) {
            errors.add("Degisken sayisi cok az: " + count + " (min: " + props.validation().minVariables() + ")");
        }
        if (count > props.validation().maxVariables()) {
            log.warn("Degisken sayisi {} maksimum {}'u asiyor, ilk {} tanesi alindi",
                    count, props.validation().maxVariables(), props.validation().maxVariables());
            variables = variables.subList(0, props.validation().maxVariables());
        }

        // Placeholder-degisken eslesme kontrolu
        if (bodyHtml != null && !variables.isEmpty()) {
            Set<String> placeholdersInHtml = PLACEHOLDER.matcher(bodyHtml).results()
                    .map(m -> m.group(1))
                    .collect(Collectors.toSet());

            Set<String> variableKeys = variables.stream()
                    .map(TemplateVariableDto::key)
                    .collect(Collectors.toSet());

            // HTML'de olup degiskenlerde olmayan placeholder'lar
            Set<String> missingVars = new HashSet<>(placeholdersInHtml);
            missingVars.removeAll(variableKeys);
            if (!missingVars.isEmpty()) {
                log.warn("HTML'de tanimli olmayan degiskenler var: {}", missingVars);
            }

            // Degiskenlerde olup HTML'de olmayan key'ler
            Set<String> unusedVars = new HashSet<>(variableKeys);
            unusedVars.removeAll(placeholdersInHtml);
            if (!unusedVars.isEmpty()) {
                log.warn("Tanimli ama HTML'de kullanilmayan degiskenler: {}", unusedVars);
            }
        }

        return variables;
    }

    private String extractString(JsonNode root, String field, List<String> errors) {
        if (!root.has(field) || root.get(field).isNull()) {
            errors.add(field + " eksik");
            return null;
        }
        JsonNode node = root.get(field);
        if (!node.isTextual()) {
            errors.add(field + " string olmali");
            return null;
        }
        return node.asText();
    }
}
