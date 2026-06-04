package com.muhur.signature.controller;

import com.muhur.signature.dto.request.OtpVerifyRequest;
import com.muhur.signature.dto.request.SignatureAcceptRequest;
import com.muhur.signature.dto.request.SignatureRejectRequest;
import com.muhur.signature.dto.response.SignatureViewResponse;
import com.muhur.signature.service.SignatureDecisionService;
import com.muhur.signature.service.SignatureEvidence;
import com.muhur.signature.service.SignaturePublicViewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/sign")
@RequiredArgsConstructor
public class PublicSignatureController {

    private final SignaturePublicViewService viewService;
    private final SignatureDecisionService decisionService;

    @GetMapping("/{token}")
    public SignatureViewResponse view(@PathVariable String token) {
        return viewService.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token,
                                       @RequestBody(required = false) SignatureAcceptRequest body,
                                       HttpServletRequest http) {
        decisionService.accept(token, toEvidence(body, http));
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

    static SignatureEvidence toEvidence(SignatureAcceptRequest body, HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        String ip = (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim()
                                                    : http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");
        boolean consent = body != null && body.consent();
        return new SignatureEvidence(ip, ua, consent);
    }
}
