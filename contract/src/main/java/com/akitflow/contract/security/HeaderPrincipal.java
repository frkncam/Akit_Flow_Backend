package com.akitflow.contract.security;

import java.security.Principal;

/**
 * Authenticated user info passed from api-gateway via trusted headers.
 * Replaces the JWT-based Jwt principal — contract-service no longer
 * validates JWTs directly.
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
