package com.muhur.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * After successful JWT validation, this filter extracts user claims and
 * injects them as trusted headers for downstream services. The original
 * Authorization header is stripped so JWT secrets never leave the gateway.
 *
 * Headers emitted to downstream:
 *   X-User-Id     ← sub claim
 *   X-Org-Id      ← organizationId claim
 *   X-User-Email  ← email claim
 *   X-User-Role   ← role claim (e.g. OWNER, ADMIN, MEMBER)
 *
 * Public requests (no JWT) are forwarded unchanged.
 */
@Component
@Slf4j
public class JwtToHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> mutateRequest(exchange, jwt))
                .defaultIfEmpty(stripAuthorization(exchange))
                .flatMap(chain::filter);
    }

    private ServerWebExchange mutateRequest(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.set("X-User-Id", jwt.getSubject());
                    headers.set("X-Org-Id", jwt.getClaimAsString("organizationId"));
                    headers.set("X-User-Email", jwt.getClaimAsString("email"));
                    headers.set("X-User-Role", jwt.getClaimAsString("role"));
                })
                .build();
        log.debug("JWT mapped to downstream headers: userId={}", jwt.getSubject());
        return exchange.mutate().request(mutated).build();
    }

    private ServerWebExchange stripAuthorization(ServerWebExchange exchange) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                .build();
        return exchange.mutate().request(mutated).build();
    }

    @Override
    public int getOrder() {
        // Spring Security webflux filter runs at order -100; we run after it
        return -50;
    }
}
