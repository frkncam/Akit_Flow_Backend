package com.akitflow.common.tenant;

import com.akitflow.common.security.HeaderPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantContext {

    private TenantContext() {}

    /** Current request's organization; null when there is no tenant context (public/event flows). */
    public static Long currentOrganizationId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof HeaderPrincipal p) {
            return p.organizationId();
        }
        return null;
    }
}
