package com.akitflow.contract.config;

import com.akitflow.common.security.TrustedHeaderAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/actuator/info",
                                         "/v3/api-docs/**",
                                         "/swagger-ui/**",
                                         "/swagger-ui.html",
                                         "/swagger-resources/**",
                                         "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new TrustedHeaderAuthFilter(),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
