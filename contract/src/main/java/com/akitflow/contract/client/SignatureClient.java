package com.akitflow.contract.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Instant;
import java.util.List;

@FeignClient(name = "signature-service")
public interface SignatureClient {

    @PostMapping("/api/v1/signatures/batch")
    List<SignatureDto> sendForSignature(@RequestBody BatchSignatureRequest request,
                                        @RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("X-Org-Id") Long orgId,
                                        @RequestHeader("X-User-Email") String email);

    @GetMapping("/api/v1/signatures/contracts/{contractId}")
    List<SignatureDto> listForContract(@PathVariable Long contractId,
                                       @RequestHeader("X-Org-Id") Long orgId);

    record BatchSignatureRequest(
            Long contractId,
            String contractTitle,
            Long fileId,
            String fileStorageKey,
            String fileName,
            Long organizationId,
            List<SignerRequest> signers
    ) {}

    record SignerRequest(String name, String email) {}

    record SignatureDto(
            Long id,
            Long contractId,
            Long fileId,
            String signerName,
            String signerEmail,
            String status,
            String providerName,
            Instant requestedAt,
            Instant signedAt,
            Instant rejectedAt,
            String rejectionReason,
            Instant expiresAt,
            String signedFileStorageKey,
            String signatureMetadata
    ) {}
}
