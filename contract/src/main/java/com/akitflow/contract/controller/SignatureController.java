package com.akitflow.contract.controller;

import com.akitflow.contract.dto.request.SignatureRejectRequest;
import com.akitflow.contract.dto.response.SignatureViewResponse;
import com.akitflow.contract.service.ContractSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints invoked from the signer's browser via the link in the
 * "lütfen imzalayın" email. No JWT — token in URL identifies the signer.
 */
@RestController
@RequestMapping("/api/v1/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final ContractSignatureService service;

    @GetMapping("/{token}")
    public SignatureViewResponse view(@PathVariable String token) {
        return service.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token) {
        service.accept(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/reject")
    public ResponseEntity<Void> reject(@PathVariable String token,
                                       @RequestBody(required = false) SignatureRejectRequest body) {
        service.reject(token, body != null ? body.reason() : null);
        return ResponseEntity.noContent().build();
    }
}
