package com.muhur.ai.validator;

import com.muhur.ai.config.AppProperties;
import com.muhur.ai.dto.response.GenerateTemplateResponse;
import com.muhur.ai.exception.GenerationValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class TemplateDraftValidatorTest {

    private TemplateDraftValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AppProperties.Validation validation = new AppProperties.Validation(20, 3, 102400);
        AppProperties props = new AppProperties(null, null, validation, null);
        validator = new TemplateDraftValidator(objectMapper, props);
    }

    @Test
    @DisplayName("Gecerli JSON basariyla parse edilmeli")
    void validJsonShouldPass() {
        String json = """
        {
            "name": "Depo Kiralama Sozlesmesi",
            "description": "ABC Ltd ile 1 yillik",
            "category": "RENTAL",
            "bodyHtml": "<h1>KIRA SOZLESMESI</h1><p>Kiraya veren: {{ lessor_name }}</p><p>Kira: {{ monthly_rent }}</p><p>Sure: {{ duration_months }}</p>",
            "variables": [
                {"key": "lessor_name", "label": "Kiraya Veren", "type": "TEXT", "required": true, "position": 1},
                {"key": "monthly_rent", "label": "Aylik Kira", "type": "CURRENCY", "required": true, "position": 2},
                {"key": "duration_months", "label": "Sure (Ay)", "type": "NUMBER", "required": true, "position": 3}
            ]
        }
        """;

        GenerateTemplateResponse response = validator.validateAndParse(json);

        assertThat(response.name()).isEqualTo("Depo Kiralama Sozlesmesi");
        assertThat(response.category()).isEqualTo("RENTAL");
        assertThat(response.variables()).hasSize(3);
        assertThat(response.variables().get(0).key()).isEqualTo("lessor_name");
    }

    @Test
    @DisplayName("Gecersiz JSON firlatmali")
    void invalidJsonShouldThrow() {
        String json = "bu json degil {{{";

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(GenerationValidationException.class)
                .hasMessageContaining("gecerli JSON degil");
    }

    @Test
    @DisplayName("Eksik zorunlu alanlar hata vermeli")
    void missingRequiredFieldsShouldThrow() {
        String json = "{\"name\": \"Test\"}";

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Gecersiz kategori OTHER'a dusmeli (hata degil)")
    void invalidCategoryShouldFallbackToOther() {
        String json = """
        {
            "name": "Test", "category": "INVALID_TYPE",
            "bodyHtml": "<p>{{ var1 }}</p><p>{{ var2 }}</p><p>{{ var3 }}</p>",
            "variables": [
                {"key": "var1", "label": "V1", "type": "TEXT", "required": true, "position": 1},
                {"key": "var2", "label": "V2", "type": "TEXT", "required": true, "position": 2},
                {"key": "var3", "label": "V3", "type": "TEXT", "required": true, "position": 3}
            ]
        }
        """;

        GenerateTemplateResponse response = validator.validateAndParse(json);
        assertThat(response.category()).isEqualTo("OTHER");
    }

    @Test
    @DisplayName("<script> etiketi reddedilmeli")
    void scriptTagShouldBeRejected() {
        String json = """
        {
            "name": "Test", "category": "RENTAL",
            "bodyHtml": "<h1>Test</h1><script>alert('xss')</script><p>{{ v1 }}</p><p>{{ v2 }}</p><p>{{ v3 }}</p>",
            "variables": [
                {"key": "v1", "label": "V1", "type": "TEXT", "required": true, "position": 1},
                {"key": "v2", "label": "V2", "type": "TEXT", "required": true, "position": 2},
                {"key": "v3", "label": "V3", "type": "TEXT", "required": true, "position": 3}
            ]
        }
        """;

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(GenerationValidationException.class)
                .hasMessageContaining("script");
    }

    @Test
    @DisplayName("3'ten az degisken hata vermeli")
    void lessThan3VariablesShouldThrow() {
        String json = """
        {
            "name": "Test", "category": "RENTAL",
            "bodyHtml": "<p>{{ v1 }}</p>",
            "variables": [
                {"key": "v1", "label": "V1", "type": "TEXT", "required": true, "position": 1}
            ]
        }
        """;

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(GenerationValidationException.class)
                .hasMessageContaining("cok az");
    }

    @Test
    @DisplayName("Duplicate key'ler filtrelenmeli")
    void duplicateKeysShouldBeFiltered() {
        String json = """
        {
            "name": "Test", "category": "RENTAL",
            "bodyHtml": "<p>{{ v1 }}</p><p>{{ v2 }}</p><p>{{ v3 }}</p>",
            "variables": [
                {"key": "v1", "label": "V1", "type": "TEXT", "required": true, "position": 1},
                {"key": "v1", "label": "Duplicate", "type": "TEXT", "required": false, "position": 2},
                {"key": "v2", "label": "V2", "type": "TEXT", "required": true, "position": 3},
                {"key": "v3", "label": "V3", "type": "TEXT", "required": true, "position": 4}
            ]
        }
        """;

        GenerateTemplateResponse response = validator.validateAndParse(json);
        assertThat(response.variables()).hasSize(3);
    }

    @Test
    @DisplayName("Gecersiz type TEXT'e dusmeli")
    void invalidTypeShouldFallbackToText() {
        String json = """
        {
            "name": "Test", "category": "RENTAL",
            "bodyHtml": "<p>{{ v1 }}</p><p>{{ v2 }}</p><p>{{ v3 }}</p>",
            "variables": [
                {"key": "v1", "label": "V1", "type": "MONEY", "required": true, "position": 1},
                {"key": "v2", "label": "V2", "type": "TEXT", "required": true, "position": 2},
                {"key": "v3", "label": "V3", "type": "TEXT", "required": true, "position": 3}
            ]
        }
        """;

        GenerateTemplateResponse response = validator.validateAndParse(json);
        assertThat(response.variables().get(0).type()).isEqualTo("TEXT");
    }
}
