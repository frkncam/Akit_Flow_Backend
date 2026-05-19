package com.akitflow.signature.controller;

import com.akitflow.signature.dto.request.SignatureRejectRequest;
import com.akitflow.signature.dto.response.SignatureViewResponse;
import com.akitflow.signature.service.SignatureDecisionService;
import com.akitflow.signature.service.SignaturePublicViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
public class PublicSignatureController {

    private final SignaturePublicViewService viewService;
    private final SignatureDecisionService decisionService;

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
