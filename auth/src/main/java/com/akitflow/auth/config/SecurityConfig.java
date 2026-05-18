package com.akitflow.auth.config;

import com.akitflow.auth.security.TrustedHeaderAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * auth-service no longer validates incoming JWTs. The api-gateway does
 * that and forwards user info as trusted headers (X-User-Id, X-Org-Id,
 * X-User-Email, X-User-Role). See TrustedHeaderAuthFilter.
 *
 * auth-service still signs (issues) JWTs at login/register/refresh via
 * JwtEncoder; for that it keeps its private RSA key.
 *
 * Public endpoints (no headers required):
 *   - POST /api/v1/auth/register, /login, /refresh, /invite/accept
 *   - GET  /oauth2/jwks (public key for downstream services)
 *   - GET  /actuator/health
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/invite/accept",
                                "/actuator/health",
                                "/oauth2/jwks",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new TrustedHeaderAuthFilter(),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
