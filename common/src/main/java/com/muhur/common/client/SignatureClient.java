package com.muhur.common.client;

import com.muhur.common.client.dto.BatchSignatureRequest;
import com.muhur.common.client.dto.SignatureDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "signature-service")
public interface SignatureClient {

    @PostMapping("/api/v1/signatures/batch")
    List<SignatureDto> sendForSignature(@RequestBody BatchSignatureRequest request,
                                        @RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("X-Org-Id") Long orgId,
                                        @RequestHeader("X-User-Email") String email,
                                        @RequestHeader("X-User-Role") String role);

    @GetMapping("/api/v1/signatures/contracts/{contractId}")
    List<SignatureDto> listForContract(@PathVariable Long contractId,
                                       @RequestHeader("X-User-Id") Long userId,
                                       @RequestHeader("X-Org-Id") Long orgId,
                                       @RequestHeader("X-User-Role") String role);
}
