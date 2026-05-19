package com.akitflow.contract.service;

import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.domain.ContractTemplate;
import com.akitflow.contract.domain.TemplateVariableData;
import com.akitflow.contract.dto.request.GeneratePdfRequest;
import com.akitflow.contract.dto.response.ContractFileResponse;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.mapper.ContractFileMapper;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.repository.ContractTemplateRepository;
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

@Service
@RequiredArgsConstructor
public class ContractPdfServiceImpl implements ContractPdfService {

    private final ContractRepository contractRepository;
    private final ContractTemplateRepository templateRepository;
    private final ContractFileRepository fileRepository;
    private final ContractFileMapper fileMapper;
    private final PdfRenderingService pdfRenderingService;
    private final PartyJsonService partyJsonService;
    private final MinioService minioService;
    private final TemplateRenderer templateRenderer;

    @Override
    @Transactional
    public ContractFileResponse generatePdf(Long contractId,
                                            Long templateId,
                                            GeneratePdfRequest body,
                                            HeaderPrincipal user) {
        Long orgId = user.organizationId();

        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + contractId));
        ContractTemplate template = templateRepository.findByIdAndOrganizationId(templateId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Template bulunamadı: id=" + templateId));

        Map<String, Object> contractView = buildContractView(contract);
        Map<String, String> systemBindings = flattenForBinding(contractView);

        List<TemplateVariableData> variables = template.getVariables() != null
                ? template.getVariables()
                : Collections.emptyList();
        Map<String, String> customValues = body != null && body.customValues() != null
                ? body.customValues()
                : Collections.emptyMap();
        Map<String, String> resolved = templateRenderer.resolveValues(variables, systemBindings, customValues);

        String substituted = templateRenderer.substitute(template.getBodyHtml(), resolved);

        Map<String, Object> thymeleafModel = new HashMap<>();
        thymeleafModel.put("contract", contractView);
        thymeleafModel.put("generatedAt", LocalDate.now());

        String html = pdfRenderingService.renderHtml(substituted, thymeleafModel);
        byte[] pdf = pdfRenderingService.htmlToPdf(html);

        int nextVersion = fileRepository.findMaxVersionByContractId(contractId) + 1;
        String safeTpl = template.getName().replaceAll("[^A-Za-z0-9._-]", "_");
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
