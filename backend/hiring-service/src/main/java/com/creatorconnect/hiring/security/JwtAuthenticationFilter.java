package com.creatorconnect.hiring.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Servlet filter that authenticates requests carrying a {@code Bearer} JWT.
 *
 * <p>Wired into the {@code SecurityFilterChain} by {@code SecurityBeansConfig}
 * and kept from auto-registering as a servlet-level filter by a disabled
 * {@code FilterRegistrationBean}.
 *
 * <p>Behaviour:
 * <ol>
 *   <li>Reads the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Verifies the token signature/expiry/issuer via {@link JwtService}.</li>
 *   <li>On success, populates the {@code SecurityContext} with a
 *       {@link UsernamePasswordAuthenticationToken} whose principal is a
 *       {@link HiringPrincipal} (userId + email + role) — the userId claim is
 *       the identity every application operation is scoped to.</li>
 *   <li>Invalid or absent tokens are ignored — downstream authorization rules
 *       decide whether the request is allowed (protected routes then yield
 *       {@code 401} via the entry point).</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /**
     * Creates the filter with its validator.
     *
     * @param jwtService the JWT validator
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                UUID userId = jwtService.extractUserId(token);
                String email = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);
                setAuthentication(new HiringPrincipal(userId, email, role));
            } else {
                // Token present but invalid (e.g. expired or wrong issuer) —
                // never carry forward a stale identity.
                SecurityContextHolder.clearContext();
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Stores the authenticated principal (userId + role authority) in the
     * security context.
     *
     * @param principal the verified token identity
     */
    private void setAuthentication(HiringPrincipal principal) {
        List<GrantedAuthority> authorities = (principal.role() == null || principal.role().isBlank())
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
