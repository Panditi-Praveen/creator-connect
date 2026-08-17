package com.creatorconnect.ai.config;

import com.creatorconnect.ai.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.ai.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the AI Service.
 *
 * <p>Mirrors the sibling services: bearer-token driven, stateless, CSRF
 * disabled. The {@code /ai/discover} endpoint is protected (a valid JWT issued
 * by the Auth Service is required, and the caller's token is forwarded to the
 * Profile Service on the outbound Feign call). The public surface is limited
 * to:
 * <ul>
 *   <li>{@code GET /ai/status} — lightweight service status check</li>
 *   <li>{@code /actuator/**} — health checks &amp; metrics</li>
 *   <li>{@code /swagger-ui/**} and {@code /v3/api-docs/**} — API documentation</li>
 * </ul>
 *
 * <p>The {@link JwtAuthenticationFilter} is placed before Spring Security's
 * {@code UsernamePasswordAuthenticationFilter} so every request that carries a
 * valid {@code Authorization: Bearer} token gets its {@code SecurityContext}
 * populated with the token's {@code userId} / role before authorization runs.
 *
 * <p>A disabled {@link FilterRegistrationBean} keeps Spring Boot from also
 * auto-registering the JWT filter as a servlet-level filter.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * Defines which endpoints are reachable without authentication.
     *
     * <p>Only the status endpoint, actuator, Swagger UI and OpenAPI docs are
     * public; every other route (including {@code /ai/discover}) requires a
     * valid JWT. Unauthenticated access to protected routes yields a
     * {@code 401} JSON body via {@link JwtAuthenticationEntryPoint}.
     *
     * @param http          the {@link HttpSecurity} builder
     * @param jwtFilter     the bearer-token authentication filter
     * @param entryPoint    the {@code 401} JSON writer
     * @return the configured security filter chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   JwtAuthenticationEntryPoint entryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling ->
                        handling.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/ai/status",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Disables auto-registration of the {@link JwtAuthenticationFilter} as a
     * servlet-level filter (it is wired into the {@code SecurityFilterChain}
     * explicitly).
     *
     * @param filter the JWT filter bean
     * @return a registration that disables servlet-level filtering
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
