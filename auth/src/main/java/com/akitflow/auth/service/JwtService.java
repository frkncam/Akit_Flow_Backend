package com.akitflow.auth.service;

import com.akitflow.auth.config.JwtProperties;
import com.akitflow.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * auth-service signs access tokens here. JWT validation (decoding) is
 * done by the api-gateway — see /oauth2/jwks endpoint exposed by this
 * service for public key distribution.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * User entity'den RS256 imzalı access token üretir.
     * Claims: sub, organizationId, email, role
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtProperties.accessTokenExpiry()))
                .claim("organizationId", user.getOrganization().getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * SecureRandom ile 256-bit opaque refresh token üretir.
     * Bu ham token client'a döner; DB'ye hash'i kaydedilir.
     */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Ham token'ı SHA-256 ile hash'ler. DB'ye kayıt ve arama için kullanılır.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Token hash'lenemedi", e);
        }
    }
}
