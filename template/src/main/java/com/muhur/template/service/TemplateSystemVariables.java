package com.muhur.template.service;

import com.muhur.template.domain.TemplateVariableType;
import com.muhur.template.dto.response.SystemVariableResponse;

import java.util.List;

public final class TemplateSystemVariables {

    private TemplateSystemVariables() {}

    public static final List<SystemVariableResponse> CATALOGUE = List.of(
            new SystemVariableResponse("contract.title", "Contract Title", TemplateVariableType.TEXT),
            new SystemVariableResponse("contract.description", "Açıklama", TemplateVariableType.MULTILINE),
            new SystemVariableResponse("contract.contractType", "Contract Type", TemplateVariableType.TEXT),
            new SystemVariableResponse("contract.status", "Durum", TemplateVariableType.TEXT),
            new SystemVariableResponse("contract.startDate", "Başlangıç Tarihi", TemplateVariableType.DATE),
            new SystemVariableResponse("contract.endDate", "Bitiş Tarihi", TemplateVariableType.DATE),
            new SystemVariableResponse("contract.value", "Değer", TemplateVariableType.CURRENCY),
            new SystemVariableResponse("contract.currency", "Para Birimi", TemplateVariableType.TEXT),
            new SystemVariableResponse("contract.creatorEmail", "Oluşturan E-posta", TemplateVariableType.TEXT),
            new SystemVariableResponse("generatedAt", "Oluşturma Tarihi", TemplateVariableType.DATE)
    );
}
