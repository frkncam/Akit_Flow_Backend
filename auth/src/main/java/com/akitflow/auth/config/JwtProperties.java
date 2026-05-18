package com.akitflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        Resource privateKey,
        Resource publicKey,
        long accessTokenExpiry,
        long refreshTokenExpiry
) {
}
