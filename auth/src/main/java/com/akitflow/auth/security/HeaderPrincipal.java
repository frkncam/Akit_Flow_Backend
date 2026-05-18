package com.akitflow.auth.security;

import java.security.Principal;

/**
 * Authenticated user info passed from api-gateway via trusted headers.
 * Replaces the JWT-based Jwt principal — auth-service no longer validates
 * incoming JWTs (only signs new ones via JwtEncoder during login).
 */
public record HeaderPrincipal(
        Long userId,
        Long organizationId,
        String email,
        String role
) implements Principal {

    @Override
    public String getName() {
        return userId == null ? null : userId.toString();
    }
}
