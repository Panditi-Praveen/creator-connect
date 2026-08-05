package com.creatorconnect.project.config;

import com.creatorconnect.project.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.project.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Project Service.
 *
 * <p>Every endpoint is authenticated except the infrastructure endpoints
 * needed by the platform:
 * <ul>
 *   <li>{@code /actuator/**} — health checks &amp; metrics</li>
 *   <li>{@code /swagger-ui/**} and {@code /v3/api-docs/**} — API documentation</li>
 * </ul>
 *
 * <p>The {@link JwtAuthenticationFilter} is placed before Spring Security's
 * {@code UsernamePasswordAuthenticationFilter} so every request that carries a
 * valid {@code Authorization: Bearer} token gets its {@code SecurityContext}
 * populated with the token's {@code userId} / role before authorization runs.
 * CSRF is disabled and sessions are stateless because the API is bearer-token
 * driven (no cookies).
 *
 * <p>A disabled {@link FilterRegistrationBean} keeps Spring Boot from also
 * auto-registering the JWT filter as a servlet-level filter (which would run
 * it outside the security chain for every request, including permitAll paths).
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * Defines which endpoints are reachable without authentication.
     *
     * <p>Only actuator, Swagger UI and OpenAPI docs are public; every other
     * route requires a valid JWT. Unauthenticated access to protected routes
     * yields a {@code 401} JSON body via {@link JwtAuthenticationEntryPoint}.
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
     * servlet-level filter.
     *
     * <p>The filter is wired into the {@code SecurityFilterChain} explicitly,
     * so without this guard Spring Boot would also register it for every
     * request outside the chain.
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
