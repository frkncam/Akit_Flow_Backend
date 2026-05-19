package com.akitflow.signature.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class TrustedHeaderAuthFilter extends OncePerRequestFilter {

    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_ORG_ID = "X-Org-Id";
    public static final String HDR_EMAIL = "X-User-Email";
    public static final String HDR_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String userId = req.getHeader(HDR_USER_ID);
        String role = req.getHeader(HDR_ROLE);

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            try {
                HeaderPrincipal principal = new HeaderPrincipal(
                        Long.parseLong(userId),
                        parseLongOrNull(req.getHeader(HDR_ORG_ID)),
                        req.getHeader(HDR_EMAIL),
                        role
                );
                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (NumberFormatException ignored) {
            }
        }

        chain.doFilter(req, res);
    }

    private static Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
