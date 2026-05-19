package com.akitflow.signature.controller;

import com.akitflow.signature.dto.request.BatchSignatureRequest;
import com.akitflow.signature.dto.request.SignatureRejectRequest;
import com.akitflow.signature.dto.response.SignatureResponse;
import com.akitflow.signature.dto.response.SignatureViewResponse;
import com.akitflow.signature.security.HeaderPrincipal;
import com.akitflow.signature.service.SignatureDecisionService;
import com.akitflow.signature.service.SignaturePublicViewService;
import com.akitflow.signature.service.SignatureRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureRequestService requestService;
    private final SignaturePublicViewService viewService;
    private final SignatureDecisionService decisionService;

    @PostMapping("/batch")
    public List<SignatureResponse> sendForSignature(@Valid @RequestBody BatchSignatureRequest request,
                                                    @AuthenticationPrincipal HeaderPrincipal user) {
        return requestService.sendForSignature(request, user.organizationId());
    }

    @GetMapping("/contracts/{contractId}")
    public List<SignatureResponse> listForContract(@PathVariable Long contractId,
                                                   @AuthenticationPrincipal HeaderPrincipal user) {
        return viewService.listForContract(contractId, user.organizationId());
    }

    @GetMapping("/{token}")
    public SignatureViewResponse view(@PathVariable String token) {
        return viewService.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token) {
        decisionService.accept(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/reject")
    public ResponseEntity<Void> reject(@PathVariable String token,
                                       @RequestBody(required = false) SignatureRejectRequest body) {
        decisionService.reject(token, body != null ? body.reason() : null);
        return ResponseEntity.noContent().build();
    }
}
