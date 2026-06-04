package com.muhur.template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateGenerateRequest(
        @NotBlank(message = "Aciklama zorunludur")
        @Size(max = 2000, message = "Aciklama en fazla 2000 karakter olabilir")
        String prompt,

        @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
        String language
) {}
