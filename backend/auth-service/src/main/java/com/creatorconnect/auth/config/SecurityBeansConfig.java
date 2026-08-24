package com.creatorconnect.auth.config;

import com.creatorconnect.auth.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.auth.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Auth Service.
 *
 * <ul>
 *   <li><b>{@link PasswordEncoder}</b> — a {@link BCryptPasswordEncoder}
 *       (strength 10) used to hash user passwords before persistence.</li>
 *   <li><b>{@link SecurityFilterChain}</b> — a stateless chain that permits the
 *       public registration/login endpoints (plus actuator health/info and
 *       Swagger UI) while keeping every other route (including
 *       {@code /auth/users/{userId}}) authenticated via a JWT bearer token.
 *       Unauthenticated access returns a JSON 401 via
 *       {@link JwtAuthenticationEntryPoint}.</li>
 *   <li><b>{@link #jwtAuthenticationFilterRegistration(JwtAuthenticationFilter)}</b>
 *       — keeps Spring Boot from auto-registering the prepared JWT filter as a
 *       servlet-level filter (it is wired into the chain explicitly).</li>
 * </ul>
 *
 * <p>CSRF is disabled because the API is stateless (no cookies) and all
 * subsequent requests are authenticated with bearer tokens.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * Password hashing strategy for the whole service.
     *
     * @return a {@link BCryptPasswordEncoder} with default strength 10
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines which endpoints are reachable without authentication.
     *
     * <p>Only registration, login, actuator health/info, and Swagger are
     * public. Every other route (including {@code /auth/users/{userId}})
     * requires a valid JWT. Unauthenticated access returns a JSON 401
     * via {@link JwtAuthenticationEntryPoint}.
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
                                "/auth/register",
                                "/auth/login",
                                "/actuator/health",
                                "/actuator/info",
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
     * @param filter the prepared JWT filter bean
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
