package com.akitflow.signature.controller;

import com.akitflow.common.client.dto.BatchSignatureRequest;
import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.common.query.CommonPredicate;
import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.common.web.PageResponse;
import com.akitflow.signature.domain.Signature;
import com.akitflow.signature.dto.request.OtpVerifyRequest;
import com.akitflow.signature.dto.request.SignatureAcceptRequest;
import com.akitflow.signature.dto.request.SignatureRejectRequest;
import com.akitflow.signature.dto.response.SignatureViewResponse;
import com.akitflow.signature.service.SignatureDecisionService;
import com.akitflow.signature.service.SignatureEvidence;
import com.akitflow.signature.service.SignaturePublicViewService;
import com.akitflow.signature.service.SignatureRequestService;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    @GetMapping
    public PageResponse<SignatureDto> list(
            @CommonPredicate(root = Signature.class) Predicate predicate,
            Pageable pageable) {
        return PageResponse.from(viewService.search(predicate, pageable));
    }

    @PostMapping("/batch")
    public List<SignatureDto> sendForSignature(@Valid @RequestBody BatchSignatureRequest request,
                                                @AuthenticationPrincipal HeaderPrincipal user) {
        return requestService.sendForSignature(request, user.organizationId(), user.userId());
    }

    @GetMapping("/contracts/{contractId}")
    public List<SignatureDto> listForContract(@PathVariable Long contractId,
                                               @AuthenticationPrincipal HeaderPrincipal user) {
        return viewService.listForContract(contractId, user.organizationId());
    }

    @GetMapping("/{token}")
    public SignatureViewResponse view(@PathVariable String token) {
        return viewService.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token,
                                       @RequestBody(required = false) SignatureAcceptRequest body,
                                       HttpServletRequest http) {
        decisionService.accept(token, PublicSignatureController.toEvidence(body, http));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/reject")
    public ResponseEntity<Void> reject(@PathVariable String token,
                                       @RequestBody(required = false) SignatureRejectRequest body) {
        decisionService.reject(token, body != null ? body.reason() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/otp/request")
    public ResponseEntity<Void> requestOtp(@PathVariable String token) {
        decisionService.requestOtp(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/otp/verify")
    public ResponseEntity<Void> verifyOtp(@PathVariable String token,
                                          @RequestBody OtpVerifyRequest body) {
        decisionService.verifyOtp(token, body.code());
        return ResponseEntity.noContent().build();
    }
}
