package com.akitflow.auth.service;

import com.akitflow.auth.dto.request.LoginRequest;
import com.akitflow.auth.dto.request.RefreshTokenRequest;
import com.akitflow.auth.dto.request.RegisterRequest;
import com.akitflow.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String rawRefreshToken);
}
