package com.akitflow.contract.controller;

import com.akitflow.contract.dto.request.GeneratePdfRequest;
import com.akitflow.contract.dto.response.ContractFileResponse;
import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.contract.service.ContractFileService;
import com.akitflow.contract.service.ContractPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts/{contractId}/files")
@RequiredArgsConstructor
public class ContractFileController {

    private final ContractFileService fileService;
    private final ContractPdfService pdfService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractFileResponse> upload(@PathVariable Long contractId,
                                                       @RequestParam("file") MultipartFile file,
                                                       @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.upload(contractId, file, user));
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<ContractFileResponse> generatePdf(@PathVariable Long contractId,
                                                            @RequestParam Long templateId,
                                                            @RequestBody(required = false) GeneratePdfRequest body,
                                                            @AuthenticationPrincipal HeaderPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pdfService.generatePdf(contractId, templateId, body, user));
    }

    @GetMapping
    public List<ContractFileResponse> list(@PathVariable Long contractId,
                                           @AuthenticationPrincipal HeaderPrincipal user) {
        return fileService.list(contractId, user);
    }

    @GetMapping("/{fileId}")
    public ContractFileResponse getOne(@PathVariable Long contractId,
                                       @PathVariable Long fileId,
                                       @AuthenticationPrincipal HeaderPrincipal user) {
        return fileService.getOne(fileId, user);
    }
}
