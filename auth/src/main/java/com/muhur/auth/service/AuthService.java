package com.muhur.auth.service;

import com.muhur.auth.dto.request.LoginRequest;
import com.muhur.auth.dto.request.RefreshTokenRequest;
import com.muhur.auth.dto.request.RegisterRequest;
import com.muhur.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String rawRefreshToken);
}
