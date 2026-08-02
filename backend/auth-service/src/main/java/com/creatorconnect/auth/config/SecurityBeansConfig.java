package com.creatorconnect.auth.config;

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

/**
 * Spring Security configuration for the Auth Service.
 *
 * <p>Day 4 scope — exposes two beans:
 * <ul>
 *   <li><b>{@link PasswordEncoder}</b> — a {@link BCryptPasswordEncoder}
 *       (strength 10) used to hash user passwords before persistence. BCrypt is
 *       intentionally slow and salted, which is the recommended choice for
 *       password storage.</li>
 *   <li><b>{@link SecurityFilterChain}</b> — a stateless chain that permits the
 *       public registration/login endpoints (plus actuator health and Swagger
 *       UI) while keeping every other route authenticated. This is the Day 4/5
 *       baseline; once protected endpoints land, the chain is tightened to
 *       enforce bearer tokens via the prepared {@link JwtAuthenticationFilter}.</li>
 *   <li><b>{@link #jwtAuthenticationFilterRegistration(JwtAuthenticationFilter)}</b>
 *       — keeps Spring Boot from auto-registering the prepared JWT filter as a
 *       servlet-level filter before Day 6 wires it into the chain.</li>
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
     * @param http the {@link HttpSecurity} builder
     * @return the configured security filter chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * Disables auto-registration of the prepared {@link JwtAuthenticationFilter}.
     *
     * <p>Without this guard, Spring Boot treats any {@code Filter} bean in the
     * context as a servlet-level filter and runs it for every request. The JWT
     * filter is intentionally not active until Day 6 wires it into the chain,
     * so it is registered with {@code enabled = false} and only becomes a
     * usable dependency.
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
