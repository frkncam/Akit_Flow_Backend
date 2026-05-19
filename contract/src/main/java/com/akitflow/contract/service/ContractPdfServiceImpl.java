package com.akitflow.contract.service;

import com.akitflow.contract.client.TemplateClient;
import com.akitflow.contract.client.TemplateClient.TemplateDto;
import com.akitflow.contract.client.TemplateClient.VariableDto;
import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.dto.request.GeneratePdfRequest;
import com.akitflow.contract.dto.response.ContractFileResponse;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.mapper.ContractFileMapper;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContractPdfServiceImpl implements ContractPdfService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-z][a-z0-9_]*)\\s*\\}\\}");

    private final ContractRepository contractRepository;
    private final ContractFileRepository fileRepository;
    private final ContractFileMapper fileMapper;
    private final PdfRenderingService pdfRenderingService;
    private final PartyJsonService partyJsonService;
    private final MinioService minioService;
    private final TemplateClient templateClient;

    @Override
    @Transactional
    public ContractFileResponse generatePdf(Long contractId,
                                            Long templateId,
                                            GeneratePdfRequest body,
                                            HeaderPrincipal user) {
        Long orgId = user.organizationId();

        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + contractId));
        TemplateDto template = templateClient.getTemplate(templateId, orgId);
        if (template == null) {
            throw new ResourceNotFoundException("Template bulunamadı: id=" + templateId);
        }

        Map<String, Object> contractView = buildContractView(contract);
        Map<String, String> systemBindings = flattenForBinding(contractView);

        List<VariableDto> variables = template.variables() != null
                ? template.variables()
                : Collections.emptyList();
        Map<String, String> customValues = body != null && body.customValues() != null
                ? body.customValues()
                : Collections.emptyMap();
        Map<String, String> resolved = resolveValues(variables, systemBindings, customValues);

        String substituted = substitute(template.bodyHtml(), resolved);

        Map<String, Object> thymeleafModel = new HashMap<>();
        thymeleafModel.put("contract", contractView);
        thymeleafModel.put("generatedAt", LocalDate.now());

        String html = pdfRenderingService.renderHtml(substituted, thymeleafModel);
        byte[] pdf = pdfRenderingService.htmlToPdf(html);

        int nextVersion = fileRepository.findMaxVersionByContractId(contractId) + 1;
        String safeTpl = template.name().replaceAll("[^A-Za-z0-9._-]", "_");
        String storageKey = "%d/%d/v%d-template-%s.pdf".formatted(orgId, contractId, nextVersion, safeTpl);
        String fileName = safeTpl + ".pdf";

        minioService.upload(storageKey, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");

        ContractFile saved = fileRepository.save(ContractFile.builder()
                .contract(contract)
                .fileName(fileName)
                .contentType("application/pdf")
                .size((long) pdf.length)
                .storageKey(storageKey)
                .version(nextVersion)
                .uploadedBy(user.userId())
                .build());

        return withDownloadUrl(saved);
    }

    private Map<String, String> resolveValues(List<VariableDto> variables,
                                              Map<String, String> systemBindings,
                                              Map<String, String> customValues) {
        Map<String, String> resolved = new HashMap<>();
        for (VariableDto v : variables) {
            String value = null;
            if (customValues.containsKey(v.key())) {
                value = customValues.get(v.key());
            } else if ("SYSTEM".equals(v.source()) && v.systemBinding() != null
                    && systemBindings.containsKey(v.systemBinding())) {
                value = systemBindings.get(v.systemBinding());
            } else if (v.defaultValue() != null) {
                value = v.defaultValue();
            }
            if (value != null) {
                resolved.put(v.key(), value);
            }
        }
        return resolved;
    }

    private String substitute(String source, Map<String, String> values) {
        if (source == null || source.isEmpty()) return source;
        Matcher m = PLACEHOLDER.matcher(source);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String replacement = values.get(key);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement != null ? replacement : m.group(0)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private Map<String, Object> buildContractView(Contract c) {
        Map<String, Object> v = new HashMap<>();
        v.put("id", c.getId());
        v.put("title", c.getTitle());
        v.put("description", c.getDescription());
        v.put("contractType", c.getContractType());
        v.put("status", c.getStatus());
        v.put("parties", partyJsonService.deserialize(c.getParties()));
        v.put("startDate", c.getStartDate());
        v.put("endDate", c.getEndDate());
        v.put("signedAt", c.getSignedAt());
        v.put("value", c.getValue());
        v.put("currency", c.getCurrency());
        v.put("creatorEmail", c.getCreatorEmail());
        return v;
    }

    private Map<String, String> flattenForBinding(Map<String, Object> contractView) {
        Map<String, String> out = new HashMap<>();
        contractView.forEach((k, val) -> out.put("contract." + k, Objects.toString(val, "")));
        out.put("generatedAt", LocalDate.now().toString());
        return out;
    }

    private ContractFileResponse withDownloadUrl(ContractFile f) {
        ContractFileResponse base = fileMapper.toResponse(f);
        String url = minioService.presignedGetUrl(f.getStorageKey());
        return new ContractFileResponse(
                base.id(),
                base.contractId(),
                base.fileName(),
                base.contentType(),
                base.size(),
                base.version(),
                base.uploadedBy(),
                base.uploadedAt(),
                url
        );
    }
}
