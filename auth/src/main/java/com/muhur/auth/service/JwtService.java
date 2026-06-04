package com.muhur.auth.service;

import com.muhur.auth.domain.User;

/**
 * auth-service signs access tokens here. JWT validation (decoding) is
 * done by the api-gateway — see /oauth2/jwks endpoint exposed by this
 * service for public key distribution.
 */
public interface JwtService {

    /**
     * User entity'den RS256 imzalı access token üretir.
     * Claims: sub, organizationId, email, role
     */
    String generateAccessToken(User user);

    /**
     * SecureRandom ile 256-bit opaque refresh token üretir.
     * Raw token returned to client; hash is persisted in the database.
     */
    String generateRefreshToken();

    /**
     * Ham token'ı SHA-256 ile hash'ler. DB'ye kayıt ve arama için kullanılır.
     */
    String hashToken(String rawToken);
}
