package com.akitflow.auth.controller;

import com.akitflow.auth.dto.request.*;
import com.akitflow.auth.dto.response.AuthResponse;
import com.akitflow.auth.dto.response.MessageResponse;
import com.akitflow.auth.security.HeaderPrincipal;
import com.akitflow.auth.service.AuthService;
import com.akitflow.auth.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InviteService inviteService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse invite(@Valid @RequestBody InviteRequest request,
                                  @AuthenticationPrincipal HeaderPrincipal user) {
        inviteService.invite(request, user.userId(), user.organizationId());
        return new MessageResponse("Davet gönderildi.");
    }

    @PostMapping("/invite/accept")
    public ResponseEntity<AuthResponse> acceptInvite(@Valid @RequestBody InviteAcceptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inviteService.acceptInvite(request));
    }
}
