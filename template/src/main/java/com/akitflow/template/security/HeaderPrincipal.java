package com.akitflow.template.security;

import java.security.Principal;

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
