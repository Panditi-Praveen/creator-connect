package com.creatorconnect.auth.config;

import com.creatorconnect.auth.security.JwtAuthenticationEntryPoint;
import com.creatorconnect.auth.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 * <p>This is the single source of truth for HTTP security. It exposes:
 * <ul>
 *   <li><b>{@link PasswordEncoder}</b> — a {@link BCryptPasswordEncoder}
 *       (strength 10) used to hash user passwords before persistence. BCrypt is
 *       intentionally slow and salted, which is the recommended choice for
 *       password storage.</li>
 *   <li><b>{@link AuthenticationManager}</b> — the global manager. Spring Boot
 *       collects every {@code AuthenticationProvider} bean from the context
 *       (including {@code security.JwtAuthenticationProvider}) and exposes them
 *       through {@link AuthenticationConfiguration}.</li>
 *   <li><b>{@link SecurityFilterChain}</b> — a stateless, CSRF-free chain that
 *       permits the public endpoints ({@code /auth/register}, {@code /auth/login},
 *       actuator, Swagger UI and OpenAPI docs) and requires authentication for
 *       everything else. The {@link JwtAuthenticationFilter} runs before
 *       {@link UsernamePasswordAuthenticationFilter} and populates the
 *       {@code SecurityContext} from a validated {@code Bearer} JWT;
 *       unauthenticated access to protected routes is answered with a clean
 *       {@code 401} JSON body by the {@link JwtAuthenticationEntryPoint}.</li>
 *   <li><b>{@link #jwtAuthenticationFilterRegistration(JwtAuthenticationFilter)}</b>
 *       — disables Spring Boot's servlet-level auto-registration of the JWT
 *       filter so it executes exactly once, inside the security chain, instead
 *       of twice per request.</li>
 * </ul>
 *
 * <p>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} /
 * {@code @Secured} so the role authorities embedded in JWTs can be enforced on
 * service methods once role-based access control lands.
 *
 * <p>CSRF is disabled because the API is stateless (no cookies, no sessions)
 * and all subsequent requests are authenticated with bearer tokens.
 */
@Configuration
@EnableMethodSecurity
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
     * Exposes the global {@link AuthenticationManager}.
     *
     * <p>Spring Boot's {@link AuthenticationConfiguration} builds this manager
     * from every {@code AuthenticationProvider} bean in the context, so
     * {@code security.JwtAuthenticationProvider} is automatically included.
     *
     * @param configuration the Spring-managed authentication configuration
     * @return the configured authentication manager
     * @throws Exception when the manager cannot be constructed
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Defines the HTTP security rule set for the whole service.
     *
     * @param http                       the {@link HttpSecurity} builder
     * @param jwtAuthenticationFilter    the bearer-token filter
     * @param jwtAuthenticationEntryPoint the 401 JSON entry point
     * @return the configured security filter chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/actuator/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Disables Spring Boot's automatic servlet-level registration of the JWT
     * filter.
     *
     * <p>Without this guard, Spring Boot treats the {@code JwtAuthenticationFilter}
     * bean as a servlet filter and executes it for every request <em>in
     * addition to</em> its position inside the security chain, causing it to run
     * twice. Registering it with {@code enabled = false} keeps it active only
     * where it belongs — in the {@code SecurityFilterChain}.
     *
     * @param filter the JWT filter bean wired into the security chain
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
