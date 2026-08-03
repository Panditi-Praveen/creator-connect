package com.creatorconnect.auth.security;

import com.creatorconnect.auth.entity.User;
import com.creatorconnect.auth.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that authenticates requests carrying a {@code Bearer} JWT.
 *
 * <p><b>Active:</b> this filter is wired into the {@code SecurityFilterChain}
 * in {@code config.SecurityBeansConfig}, positioned before
 * {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}.
 * A disabled {@code FilterRegistrationBean} prevents Spring Boot from also
 * registering it as a servlet-level filter, so it executes exactly once per
 * request — inside the security chain.
 *
 * <p>Behaviour:
 * <ol>
 *   <li>Skips the public endpoints ({@code /auth/register}, {@code /auth/login},
 *       actuator, Swagger/OpenAPI) so they never pay for a token lookup.</li>
 *   <li>Reads the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validates the token signature/expiry via {@link JwtService}.</li>
 *   <li>Loads the subject user and, if present and enabled, populates the
 *       {@code SecurityContext} with a {@link UsernamePasswordAuthenticationToken}
 *       carrying the user's role authority.</li>
 *   <li>Invalid or absent tokens are ignored — downstream authorization rules
 *       decide whether the request is allowed.</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Creates the filter with its collaborators.
     *
     * @param jwtService     the JWT validator
     * @param userRepository the user data access layer
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/auth/register")
                || uri.equals("/auth/login")
                || uri.startsWith("/actuator/")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            authenticateIfValid(token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validates the token and, on success, sets the authenticated principal.
     *
     * @param token the raw JWT from the authorization header
     */
    private void authenticateIfValid(String token) {
        try {
            if (jwtService.isValid(token)) {
                String email = jwtService.extractUsername(token);
                userRepository.findByEmail(email)
                        .filter(User::isEnabled)
                        .ifPresent(this::setAuthentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Stores the authenticated principal (email + role authority) in the
     * security context.
     *
     * @param user the loaded, enabled user
     */
    private void setAuthentication(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
